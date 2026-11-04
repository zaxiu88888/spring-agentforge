# 竞品调研：Prior Art（写代码前的定调文档）

> 调研日期：2026-11-04 · 方法：GitHub API 抓取 5 个项目的元数据 / 高赞 open issues（按 reactions 排序前 15）/ README，结合本项目（AgentForge）选型分析。
> 本项目背景：Spring Boot 3.5 + Spring AI 1.1 + MyBatis + PostgreSQL/pgvector + React 19 + AntD 6，单人简历项目，对标原型为本地开源项目 JChat（AgentHub）。

## 一、调研对象总览

| 项目 | Stars | 语言 | 定位 | 与我们的关系 |
|---|---|---|---|---|
| [spring-projects/spring-ai](https://github.com/spring-projects/spring-ai) | 9.6k | Java | 我们直接使用的底层框架 | 框架层：了解边界与已知坑 |
| [alibaba/spring-ai-alibaba](https://github.com/alibaba/spring-ai-alibaba) | 11.1k | Java | Spring AI 之上的 Agent 编排框架 | 编排层：手写循环之外的上层范式 |
| [langchain4j/langchain4j](https://github.com/langchain4j/langchain4j) | 13.4k | Java | JVM 最大的 LLM 应用库（20+ 模型、30+ 向量库） | 抽象设计对照组 |
| [langgenius/dify](https://github.com/langgenius/dify) | 160k | Python | LLM 应用开发平台（工作流+RAG） | 产品层：功能全景参照 |
| [lobehub/lobe-chat](https://github.com/lobehub/lobe-chat) | 83.8k | TS/React | AI 聊天框架/Agent 平台 | 前端 UX 参照 |

选样逻辑：框架层 → 编排层 → 产品层各取代表，全部为活跃维护的一线项目。

---

## 二、逐项目要点

### 1. Spring AI（官方框架）

- **能力面**：ChatClient/ChatModel 统一 API、Tool Calling、ChatMemory（MessageWindow/TokenWindow）、Advisors 拦截链、RAG（QuestionAnswerAdvisor + VectorStore 抽象）、MCP 支持。版本线：2.x 对应 Boot 4，**1.1.x 对应 Boot 3.5（我们的选择）**。
- **高赞 Issues 中的痛点**（与我们直接相关的）：
  - **#2101 [17+] Model Tool Calls not persisted in chat memory** —— 工具调用消息不进记忆是官方长期痛点。我们的原型项目自己解决了（metadata JSONB 存 toolCalls/toolResponse），**这是我们可以理直气壮讲的差异化设计**。
  - **#5464 [12+] Native semantic text chunking support** —— 语义分块官方才刚补，社区一直在裸奔。印证「分块策略」是 RAG 质量核心变量。
  - #403 [19+] Agent support / #2911 [48+] A2A Protocol —— 官方 Agent 抽象长期缺位（1.x 只有工具调用原语），这正是大家手写 ReAct 循环、也是 spring-ai-alibaba 存在的原因。**我们手写 Think-Execute 循环与生态方向一致，面试可讲清「为什么不用现成的」**。
  - #468 [19+] 向量库多 collection/索引 —— 单库多知识库隔离的真实需求，我们按 kb_id 过滤是简化版。
- **启示**：留在 1.1.x + Boot 3.5 是对的；写代码时优先复用 ChatClient/ToolCallingManager/ChatMemory 原语，不自造底层轮子，自造的只有「循环与编排」这一层。

### 2. Spring AI Alibaba（编排层）

- **架构分三层**：Admin（可视化平台）→ **Agent Framework**（ReactAgent + Context Engineering）→ **Graph/StateGraph**（持久化工作流运行时：条件路由/嵌套图/并行/状态管理）。
- **内置多 Agent 模式**：`SequentialAgent / ParallelAgent / RoutingAgent / LoopAgent`。
- **Context Engineering 清单**（README 明列的最佳实践）：human-in-the-loop、context compaction、context editing、model & tool call limit、tool retry、planning、dynamic tool selection —— **这就是一个现成的「Agent 可靠性 feature 清单」**，我们至少应覆盖：tool call limit（已有 MAX_STEPS）、tool retry、context compaction（超窗摘要）。
- **高赞 Issues**：#4424 [5+] 请求 AG-UI 协议（Agent 调试 UI）；大量 PR 集中在 graph 状态/HITL/resume 边界修复 —— 说明**图编排 + 人机协同是大厂投入重兵的方向，也是 bug 重灾区**。
- **启示**：MVP 不做图编排（复杂度爆炸），但把 Agent 循环写成「状态机 + 清晰的扩展点」，为未来升级留缝；Context Engineering 清单挑两三个低成本项做（见拓展节）。

### 3. LangChain4j（抽象对照组）

- **核心抽象**：统一 API（20+ 模型/30+ 向量库）；AI Services 声明式接口；RAG 五段流水线 `DocumentParser → DocumentSplitter → EmbeddingStore → ContentRetriever → RetrievalAugmentor + ContentInjector`。
- **高赞 Issues 痛点**：
  - **#1081 [10+] Support more chunking strategies** —— 分块策略多样性是刚需（我们目前只有「按 Markdown 标题切」一种）。
  - **#4135 [5+] Tools require human approval（accept/decline）** —— 高危工具人工确认。
  - #1183 [4+] Async Tool Execution；#2968 [3+] 无法中途停止 AI 请求 —— **可中断性/取消**是 Agent 交互的常见缺口，我们原型的 SSE + @Async 也一样没有取消机制。
  - #2331 [11+] OpenTelemetry 可观测性 / Langfuse tracing（多个 issue 反复出现）—— **追踪/观测是跨项目的第一共同诉求**。
- **启示**：RAG 部分借鉴其「流水线接口化」思想：定义 `Splitter` / `Retriever` 小接口而非写死过程，简历上能讲清「为什么这样分层」；中文场景 + 混合检索（向量+关键词）值得做，LangChain4j 文档同样强调 retriever 可组合。

### 4. Dify（产品层，160k stars）

- **功能全景**：可视化工作流画布、RAG pipeline（多格式文档/分段策略/检索测试/召回调试）、数百模型接入、观测集成（Langfuse/Opik/Phoenix）、MCP、团队协作。
- **值得借鉴的产品设计**：
  - **知识库侧的「检索测试/召回调试」界面**：输入 query 直接看命中 chunk 与分数 —— 对开发者和面试演示都极有价值，且实现成本极低（一个检索调试 API + 一个前端面板）。
  - 分段策略可选（固定长度/父子分段/自定义）；工作流画布 MVP 不做。
- **Issues**：高赞集中在 API 行为细节（消息编辑重生成 #36395、MCP OAuth、观测集成兼容）—— 产品做大后的复杂度都在边角，与我们无关，说明**砍功能面、做深核心链路**是对的。

### 5. LobeChat（前端 UX，83.8k stars）

- **UX 亮点**：消息流内渲染 Artifact/工具过程、会话与 Agent 市场管理、桌面端/移动端多端、主题系统、插件（技能）体系按需注入上下文。
- **Issues 相关**：#16165 [2+] SSE 流 late error（Premature close）—— SSE 长连接健壮性是普遍坑（我们原型在「未建连就发消息会 500」上也有同样问题）；#17902 子 Agent 模型不可配置 —— 模型可选性是用户刚需（我们已有 Agent 级模型选择，做对了）。
- **启示**：聊天 UI 的「过程可视化」（思考/工具调用/结果分步展示）是 LobeChat/Dify 共同的招牌交互，我们原型已有雏形（5 种 SSE 事件 + 工具卡片），继续打磨即可，不必追多端/市场类重功能。

---

## 三、问题 1：我们可以学习和参考的地方

1. **消息持久化协议**（Spring AI #2101 之痛）：assistant 消息带 `toolCalls`、tool 消息带 `toolResponse` 存 JSONB metadata，重放时按 role 还原成 Spring AI Message 类型 —— 原型已做对，照搬并补单元测试。
2. **RAG 流水线接口化**（LangChain4j）：Parser/Splitter/Retriever 各自小接口，Markdown-Splitter 只是第一个实现，为后续策略留位。
3. **Context Engineering 挑选落地**（Spring AI Alibaba）：tool retry（失败重试一次再报错给模型）、context compaction（窗口满时摘要压缩而非硬截断）。
4. **检索调试面板**（Dify）：`GET /api/knowledge-bases/{id}/search?q=` 直接返回 top-k chunk + 距离分数，前端一个小面板。演示/调试价值极高，半天工作量。
5. **SSE 健壮性**（LobeChat #16165）：SseService.send 找不到连接不再抛异常（原型缺陷），改为「未连接即跳过，消息反正已落库」；前端 onerror 做指数退避重连。
6. **模型可选**（LobeChat #17902 之痛反证）：Agent 级 model 字段 + 注册表设计保留，这是已验证的用户刚需。

## 四、问题 2：我们可以拓展的地方（按性价比排序）

| # | 拓展点 | 依据 | 成本 | 简历价值 |
|---|---|---|---|---|
| 1 | **混合检索**：pgvector 向量 + PostgreSQL 全文检索（tsvector）加权融合，可选重排序 | LangChain4j/Dify 均为标配；中文场景纯向量召回不稳 | 中 | 高（可量化对比召回） |
| 2 | **分块策略可插拔**：标题切分 / 固定 token 滑窗 / 递归切分三选一，Agent 或 KB 级配置 | Spring AI #5464、LangChain4j #1081 | 低-中 | 高 |
| 3 | **可观测性**：每步 think/execute 记录 token 用量、耗时、工具调用链入库 + 会话详情时间线 | LangChain4j #2331（11+）、Dify 集成三家观测 | 中 | 高（面试硬通货） |
| 4 | **流式中断**：前端停止按钮 → 取消正在运行的 Agent（Future.cancel + SSE 事件） | LangChain4j #2968、LobeChat #16165 | 中 | 高（体现工程完备度） |
| 5 | **工具调用审批**：高危工具（邮件/SQL）先推 SSE 审批事件，用户点允许才执行 | LangChain4j #4135（5+）、Alibaba HITL | 中 | 高（差异化亮点） |
| 6 | 多 Agent 编排（Sequential/Routing） | Spring AI Alibaba | 高 | 高但 MVP 外 |
| 7 | MCP 客户端接入（外部工具生态） | Spring AI #2912、Dify/LobeChat 均已支持 | 高 | 中（简历上不如 1-5 稳） |

**结论**：1–5 是单人可落地的拓展面；6–7 留到 MVP 之后再评估。拓展 3（可观测性）优先级最高——它同时服务调试、演示与面试叙事。

## 五、问题 3：MVP 阶段最应该做的核心骨架

判断标准：**砍掉它系统就不成立**（骨架）vs「有了更好」（拓展）。

### 必做骨架（对应 8 阶段蓝图的收敛版）

1. **工程骨架**：Maven 单模块 + Boot 3.5 + Spring AI 1.1 BOM；`application.yaml` 密钥全部走环境变量；React 19 + Vite + AntD 6 + @ant-design/x 脚手架；`.gitattributes`/`.gitignore`。
2. **数据与 CRUD 层**：6 张表（已建好）→ MyBatis Mapper/XML（UUID/JSONB/VECTOR 三类映射）→ FacadeService → Controller → `ApiResponse<T>`；覆盖 Agent/会话/消息/知识库/文档五套。
3. **多模型注册表**：`ChatClientRegistry`（Map 注入，Bean 名 = key = Agent.model 值），glm-4.6 + deepseek-chat 双模型，可运行时切换。
4. **Agent 循环（核心中的核心）**：Think-Execute 状态机；`internalToolExecutionEnabled(false)` + 手动 `ToolCallingManager`；记忆重建（clear + conversationHistory 回写）；terminate 信号工具；MAX_STEPS 上限；**修掉原型 Factory 的共享状态并发缺陷**。
5. **工具体系**：`Tool` 接口 + FIXED/OPTIONAL 分类 + `MethodToolCallbackProvider` 装配（AOP 解代理）；首批工具：terminate、KnowledgeTool（FIXED）、TimeTool、SqlQueryTool（只读）（OPTIONAL）。
6. **RAG 链路**：Markdown 上传 → 标题分块 → 智谱 embedding-3(1024) → pgvector L2 top-k → KnowledgeTool 注入。
7. **SSE 实时推送**：连接管理 + 5 类事件；**修掉原型「未连接抛异常」缺陷**；前端思考/执行状态气泡 + 工具调用卡片。
8. **测试骨架**（本次调研新增强调）：Agent 循环状态机单测（mock ChatClient）、记忆重放协议单测、分块器单测、CRUD 集成测试（Testcontainers 或连本地 PG）。

### MVP 明确不做（写入共识，防止 scope creep）

图编排/多 Agent、MCP、语音、多模态、插件市场、团队协作、移动端、Docker 部署链（可后补）。

### 骨架完成的验收标准

- 能用界面创建 Agent（选模型/勾工具/挂知识库）→ 上传 md 建知识库 → 发消息 → 看到 SSE 推送的思考/工具/回答全过程 → 刷新页面记忆完整恢复；
- `mvn test` 全绿，Agent 循环与分块器有针对性单测。

---

## 六、来源

- 仓库元数据与 Issues：GitHub API（`/repos/{owner}/{repo}`、`/issues?sort=reactions`），抓取于 2026-11-04
- README：`raw.githubusercontent.com` 各仓库默认分支
- 关键 issue 引用：spring-ai #2101/#5464/#403/#468；langchain4j #1081/#4135/#1183/#2968/#2331；spring-ai-alibaba #4424；lobe-chat #16165/#17902；dify #36395
