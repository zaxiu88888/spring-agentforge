# M7 模块讲解稿：SSE 实时推送（你先答，AI 补全）

> 对应代码：`sse/SseMessage.java`、`sse/SseServiceImpl.java`、`sse/SseAgentProgressListener.java`、`sse/ProgressListenerFactory.java`、`controller/SseController.java`。
> 本模块有一处**我自审时抓到的缺陷**（第 4 题），先自己读代码再答。

## 问题清单

1. **为什么选 SSE 而不是 WebSocket？这个项目里"服务端单向推送"够用吗？什么信号出现时必须换 WebSocket？**
2. **SseServiceImpl 用 ConcurrentHashMap 管 emitter。`connect()` 里对同会话旧连接的处理是什么？为什么必须处理？**
3. **🩹 `send()` 找不到连接返回 false 而不是抛异常（原型是抛的）——为什么这样改？消息会不会因此丢？前端怎么补偿？**
4. **SseAgentProgressListener 为什么不能做成 @Component 单例？我的第一版实现错在哪、怎么修的？（这是 M7 自审抓到的真问题）**
5. **SseEmitter 的 onCompletion/onTimeout/onError 三个回调各在什么时机触发？如果忘了注册它们会发生什么？**
6. **AgentLoop 在 @Async 线程跑、SseEmitter 由 Tomcat IO 线程创建——跨线程 send 为什么可行？SseEmitter 线程安全吗？**
7. **四种事件（THINKING/EXECUTING/GENERATED_CONTENT/DONE）里为什么 GENERATED_CONTENT 带 message 而 THINKING/EXECUTING 只带 statusText？前端分别怎么消费？**
8. **Controller 返回 SseEmitter 时 Spring MVC 做了什么？`request().asyncStarted()` 断言在验证什么行为？**

## 你的回答

（写在每题下方，AI 会批注补全。提醒：第 4 题先读 `SseAgentProgressListener.java` 类注释和 `ProgressListenerFactory.java`）

1.
2.
3.
4.
5.
6.
7.
8.

---

## 参考要点（答题后再看）

- **1**：Agent 过程推送是**纯单向**（服务端→前端），SSE 复用 HTTP、自带断线重连（EventSource）、实现零成本；WebSocket 提供双向全双工，代价是协议升级、心跳、状态管理全套复杂度。够用性判据：交互模型里客户端只需要"发 REST + 收推送"。换的信号：出现**服务端需要主动询问客户端**的场景（工具调用人工审批——LangChain4j #4135 那种 accept/decline 交互）或高频双向流（协作编辑/语音）。
- **2**：旧连接 `complete()` 后新连接覆盖 Map 条目。不处理的后果：浏览器刷新/重连后 Map 里留着旧 emitter，`send()` 发给已死连接（IO 异常被吞），新连接收不到任何事件——现象是"刷新后聊天不再更新"。complete() 触发旧 emitter 的 onCompletion，`remove(key, value)` 条件删除保证不会误删新条目。
- **3**：原型抛异常 → Agent 循环被 SSE 连接状态杀死——**传输层的临时缺失不该有权限终止业务**（用户可能只是晚开了页面；消息已落库是完整事实）。返回 false 让推送降级为"跳过"，数据不丢：前端 SSE 重连（EventSource 原生自动重连）后拉 `GET /chat-messages/session/{id}` 对账——**落库是 source of truth，SSE 只是加速器**。
- **4**：第一版我把 listener 做成了 @Component 单例，用实例字段 `currentSession` 记会话（从 onMessage 的参数里反推）——单例 + 可变实例字段 + 并发多会话 = 互相覆盖，A 会话的事件可能推给 B（和 M4 修的 Factory 缺陷同构：**单例里的会话态**）。修复：listener 按会话实例化，sessionId 走构造参数；Factory 里用 `ProgressListenerFactory.create(sessionId)` 在装配时 new——生命周期从"容器单例"改为"随 AgentLoop 实例"，天然隔离。
- **5**：onCompletion=正常完成（complete() 调用或客户端断开）；onTimeout=30 分钟无输出到期；onError=IO 异常。不注册的后果：死连接永远留在 Map 里——内存泄漏（每条死 emitter 挂着输出缓冲），且 send 持续对死连接做无用 IO。三回调统一做 `remove(key, emitter)`（带值条件删除，防并发误删新连接）。
- **6**：SseEmitter 本身**不是线程安全**的（官方文档明示），但它内部用 synchronized 保护发送（Spring 5 起对 send 加锁）；跨线程可行靠的是：emmitter 持有的是 ServletResponse 的输出引用，只要响应未完成，任意线程写入都由容器序列化到连接。真正要防的是**并发 send 交错**——我们的事件全部来自单个 AgentLoop 线程（@Async），天然串行，无需额外加锁；多 Agent 同会话的场景本项目不存在（每会话一条消息只触发一次事件）。
- **7**：GENERATED_CONTENT 的载荷是**事实**（新消息，前端要追加进消息列表渲染成气泡/工具卡片）；THINKING/EXECUTING 是**过程状态**（瞬态提示语，前端只更新一个状态气泡，消息列表不变）——一个进数据模型、一个进 UI 状态。DONE 是事实（终态信号，收起状态气泡 + 错误摘要 toast）。这种"数据事件与状态事件分离"让前端 reducer 干净：前者 append、后者 setState。
- **8**：返回 SseEmitter 后 MVC 把请求转入**异步模式**（Servlet 3 async）——Tomcat 线程立即释放去服务别的请求，响应由 emitter 的 send 驱动持续输出。`asyncStarted()` 断言验证的正是这个转换：如果实现错误地同步返回（比如在 controller 里就 complete 了），异步未开启，SSE 语义崩坏。

---

## 你的回答与批注（2026-11-06，8/8 通过结业）
1. 单向广播/轻量自带重连/换 WS 信号（高频双向、打断插话）✅ 补：工具人工审批（HITL）也是信号。
2. complete 旧连接；防泄漏+防双管乱推 ✅ 补：complete() 触发旧 onCompletion → remove(key,value) 条件删除不会误删新连接。
3. 保护后台任务；会丢；重连+拉全量补偿 ✅ 完整。升华句：**落库是 source of truth，SSE 只是加速器**。
4. 绑定会话专属 emitter / 单例字段被覆盖 / 每请求 new ✅✅（自审缺陷题过关）。
5. 三回调时机 + 内存泄漏 ✅。
6. Servlet 异步输出流 / synchronized / 乱序风险单线程顺序调用 ✅（我们的事件源天然是单 AgentLoop 线程）。
7. 事实 vs 瞬态、append vs 覆盖 ✅（我们是消息流末尾气泡而非顶部状态栏，小差异无妨）。
8. 异步模式释放 Tomcat 线程 / 验证异步挂起 ✅。
