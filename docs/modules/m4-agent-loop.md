# M4 模块讲解稿：Agent 循环（面试主战场，你先答，AI 补全）

> 对应代码：`agent/AgentLoop.java`、`agent/AgentRuntimeFactory.java`、`agent/tools/`、`event/`、`config/AsyncConfig.java`。
> 这是整个项目被追问最狠的模块——十道题全是高频面试题。

## 问题清单

1. **`internalToolExecutionEnabled(false)` 到底关掉了什么？不关的话 Agent 循环还能工作吗？**
2. **描述一次完整的 think → execute 往返：think 返回什么你会进 execute？execute 做了哪四件事？**
3. **循环有哪几条终止路径？`terminate` 工具的方法体为什么是空的？终止逻辑放在哪里、为什么不放在工具里？**
4. **execute 之后为什么要 `chatMemory.clear()` 再整体 `add(conversationHistory())`？直接往里 add 新消息会怎样？**
5. **你修复了原型项目的一个并发缺陷，具体是什么问题？怎么修的？**
6. **Agent 自己产出的消息为什么走 `agentCreateChatMessage()`（不发事件），而用户消息走 `createChatMessage()`（发事件）？如果两者反了会发生什么？**
7. **测试过程中发现过一个"工具结果重复落库"的真 bug——它是怎么发生的？你的修复思路是什么？这个 bug 对用户可见的现象会是什么？**
8. **`@Async("agentExecutor")` 的线程池为什么是 core=4/max=10/queue=100？请求超过 100 会发生什么？HTTP 请求会因为 Agent 慢而被阻塞吗？**
9. **工具返回的明明是字符串 `result-of-x`，落库后却变成 `"result-of-x"`（带引号），为什么？这对你设计工具返回值有什么影响？**
10. **单元测试里你 mock 了 ChatClient 但用了真的 ToolCallingManager——为什么这么分？全 mock 会丢掉什么？**

## 你的回答与 AI 批注（2026-11-05 完成作答）

**1. internalToolExecutionEnabled(false) 关掉了什么？**
> 用户：关掉框架底层自动跑工具的能力。不关的话框架自动接管，没法手动控制 think→execute 循环和状态管理。
>
> ✅ 对。面试时把"接管"展开成后果链更狠：不关时 `.call()` 内部会"执行工具→结果回传→再调模型"直到跑完才返回——**中间步骤全在框架肚子里**，推不了 SSE 过程事件、实施不了 MAX_STEPS、插不进审批/日志。一句话：放弃控制权换省事，我们要的是控制权。

**2. execute 做了哪四件事？**
> 用户：①解析工具参数 ②执行本地工具方法 ③封装工具结果 ④把结果塞回上下文。
>
> ⚠️ 视角错位：你答的是 **ToolCallingManager 框架内部**的流水线，不是我们 AgentLoop.execute() 的四件事。我们的四件事：①把工具回调放进 options 构造 Prompt（框架按名解析回调的前提）；②调 `executeToolCalls` 手动执行；③`chatMemory.clear() + conversationHistory()` 整体重建记忆；④**只把本轮新增的** ToolResponse 落库并检查 terminate。审题要看主语是谁的代码。

**3. 终止路径 + terminate 空方法？**
> 用户：4 条路径对；空方法因为只是信号；工具只管声明意图，主循环掌握全局状态。
>
> ✅ 好，"工具只管声明意图，主循环才掌握全局状态"可直接用于面试。再补两个层次：①工具不该感知循环的存在（职责边界，工具可独立测试/复用）；②并行工具调用时（一个 terminate + 其他工具同轮）由循环统一裁决，散在工具里没法裁决。

**4. 为什么 clear() + 整体重建？直接 add 会怎样？**
> 用户：防止上下文无限膨胀，直接 add 旧消息越堆越多 Token 爆掉。
>
> ❌ 答偏了，这是十题里必须纠准的一题。真实原因：`conversationHistory()` 返回的是"prompt 消息 + assistant(tool_calls) + tool 响应"的**完整重建序列**——直接 add 的话记忆里会有**两条** assistant(tool_calls)（记忆里原有的那份 + 重建的这份），消息序列不合法（assistant(tool_calls) 与 tool 响应的配对关系被破坏），下一轮调模型直接被 API 拒绝。"上下文膨胀"是 MessageWindowChatMemory 的 maxMessages 窗口管的另一件事。clear+重建保证序列**唯一且成对相邻**。

**5. 原型的并发缺陷？**
> 用户：通常是共享单例 ChatMemory 导致 A 用户记录串到 B；修复按 Session 隔离或每次新建。
>
> ⚠️ 猜测式回答且对象错了——面试说"通常是"会露怯，这是我们项目里**真实存在**的缺陷，要精确到类和字段：原型 Factory 是**单例 Bean 却用实例字段 agentConfig 暂存装配中间态**，并发 create() 时 A 的配置（模型/工具列表/记忆窗口）覆盖 B 的，B 的 Agent 穿了 A 的装备。Memory 并没有共享（本来就按会话隔离）。修复：中间态全部局部变量按值传递，Factory 回到无状态。

**6. 为什么 Agent 消息不发事件？**
> 用户：用户消息发事件为了通知前端刷新；Agent 不发防死循环；反了会每吐一个字触发一次事件。
>
> ⚠️ 核心对（防死循环 ✅），第一半错：用户消息发事件**不是为了通知前端**——是为了触发 `@Async` 的 Agent 执行（HTTP 线程立即返回，Agent 在 agentExecutor 线程池跑）；前端拿 AI 回复靠 SSE/拉历史，与这个事件无关。"每吐一个字"也不准确（我们是每条消息一次）；真正的灾难链是：事件→Agent 运行→落库→**又发事件→又触发 Agent** 的无限自我对话。

**7. 重复落库 bug 是怎么发生的？**
> 用户：现象：同一工具结果出现两次。原因：手动 add 一次，框架或监听器又自动 add 一次。修复：按 toolCallId 去重或统一落库入口。
>
> ❌ 现象 ✅，根因错——这题是你**项目里真实发生的 bug**，讲不准会直接露馅。真实根因：`conversationHistory()` 是**全量**历史，我最初遍历全量并把里面所有 ToolResponse 落库——第二轮 execute 时第一轮的工具响应**又被落了一遍**（maxSteps 测试的 5≠4 现形）。修复：记录执行前历史长度 `messagesBefore.size()`，只持久化从该下标起的新增段。你说的"按 toolCallId 去重"作为通用兜底可提，但我们选**边界法**：增量语义下从源头只处理新增，比事后去重干净且零成本。

**8. 线程池参数与队列满？**
> 用户：超 100 触发拒绝策略（报错或丢弃）；HTTP 不阻塞，@Async 扔线程池后主线程返回，前端靠 SSE/轮询拿结果。
>
> ✅ 好。没答参数依据：4/10/100 是"单人演示场景 4 并发够、突发 10、100 队列缓冲尖峰"的量级判断；拒绝策略是默认 Abort → 异常被监听器兜底 catch 记日志——**消息已落库，只是本次没有 AI 回复**，数据不丢。追问量级时可答：单次 Agent 运行数秒到数十秒，10 线程支撑几十并发会话无压力。

**9. 工具结果为什么带引号？**
> 用户：底层序列化（Jackson）当 JSON 字符串处理加了引号；启示：返回值直接设计成 JSON 对象/结构化。
>
> ✅ 对。机制精确版：Spring AI 把**任意**工具返回值统一 JSON 序列化进 ToolResponse（字符串→JSON 字符串）。落地：SqlQueryTool 就直接返回结构化 JSON 字符串，模型看得懂、人也好调试。对模型无害，但设计工具时要心里有数。

**10. 为什么 mock ChatClient 但用真 ToolCallingManager？**
> 用户：ChatClient 是外部大模型不确定且费钱必须 Mock；ToolCallingManager 是我们自己的核心业务必须跑真的；全 Mock 变成测 Mock 数据，漏掉参数解析/结果拼装/上下文流转的集成 bug。
>
> ⚠️ 结论好（全 mock 假绿 ✅），一处事实错误：ToolCallingManager **不是我们自己的代码，是 Spring AI 框架的类**。用真的不是因为自研，而是它是被测循环的**真实协作对象**——用真的才能验证"我们构造 Prompt 的方式真的能被框架执行"。mock ChatClient 的更本质理由：它是测试的**输入源**——按剧本控制"模型说什么"才能造出六条路径（贵/慢/不确定是表层理由）。

---

## 参考要点（答题后再看）

- **1**：关掉的是 ChatClient 内部的"收到 tool_calls → 自动执行 → 自动把结果回传模型再调一次"闭环。不关的话 `.call()` 会在框架内部把多轮工具全跑完才返回，我们的循环看不到中间过程、推不了 SSE、也控制不了步数上限——等于放弃了 Agent 的控制权。关掉后我们拿到"带 tool_calls 的原始响应"，执行与否由自己决定。
- **2**：think 返回的 AssistantMessage 带 `getToolCalls()` 非空 → 进 execute。execute 四件事：①把工具回调放进 options 构造 Prompt；②`ToolCallingManager.executeToolCalls(prompt, lastChatResponse)` 手动执行；③`clear + conversationHistory()` 整体重建记忆；④只把**本轮新增**的 ToolResponse 落库并检查 terminate。
- **3**：四条——直接回答（无 toolCalls）、terminate 响应、MAX_STEPS=20 上限、异常→ERROR。terminate 空方法：它只是给模型的"喊停出口"，真正的状态变更在主循环识别响应名。工具里改状态的问题：工具不该知道循环的存在（职责越界），且并行工具调用时由循环统一裁决更可控。
- **4**：`conversationHistory()` 返回的是"prompt 消息 + assistant(tool_calls) + tool 响应"的完整重建序列，直接 add 会导致记忆里出现两条 assistant(tool_calls)（旧记忆里那份 + 重建这份），消息序列非法，下一轮模型调用会被 API 拒绝。clear+重建保证成对相邻、序列唯一。
- **5**：原型 Factory 是单例 Bean 却用实例字段 `agentConfig` 暂存装配中间态——两个用户同时创建 Agent 时字段互相覆盖（A 的工具列表装进 B 的循环）。修复：全部中间状态收进局部变量按值传递，Factory 本身回到无状态。
- **6**：防事件风暴死循环。用户消息发事件 → 触发 Agent；若 Agent 落库也发事件 → 又触发一次 Agent → 无限自我对话。反过来的话：Agent 产出丢失（不落库）或无限循环，二选一都是灾难。
- **7**：`conversationHistory()` 是**全量**历史，我在 execute 里遍历了它并持久化所有 ToolResponse——第二轮 execute 时第一轮的工具响应又被持久化一次。可见现象：聊天记录里工具结果卡片重复出现、越晚的对话重复越多。修复：记录执行前的历史长度 `messagesBefore.size()`，只持久化从该下标开始的**新增**消息。教训：处理"增量协议"时先问数据结构是全量还是增量。
- **8**：参考了原型取值：4 个并发 Agent 够单人演示场景，突发到 10，100 队列缓冲尖峰。队列满（默认 AbortPolicy）→ 提交抛 RejectedExecutionException → 被监听器兜底 catch 记日志，用户消息已落库，只是本次没有 AI 回复。HTTP 不阻塞：`@Async` 让监听器在线程池跑，Controller 早返回了。
- **9**：Spring AI 的 ToolResponse 把工具返回值统一 JSON 序列化（字符串→JSON 字符串带引号；对象→JSON 结构）。影响：工具返回纯文本时模型看到的是带引号版本，多数情况无害；设计工具时干脆直接返回结构化 JSON 字符串，反而更可控（本项目 SqlQueryTool 就这么设计）。
- **10**：ChatClient 是 fluent 链式 API，mock 它等于控制"模型说什么"——这是单测的输入源，必须 mock 才能造六条路径。ToolCallingManager 是被测循环的真实协作对象（回调解析、执行、conversationHistory 组装），用真的才能验证"我们的 Prompt 构造方式真的能被框架执行"——全 mock 会把最关键的集成契约 mock 掉，重构 Prompt 构造时测试依然绿（假绿）。代价：对 Spring AI 内部行为有耦合，框架升级可能需要同步测试——这是值得付的钱。
