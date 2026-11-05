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

## 你的回答

（写在每题下方，AI 会批注补全）

1.
2.
3.
4.
5.
6.
7.
8.
9.
10.

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
