# 技术设计（How——写代码前的最后一道定稿）

> 上游文档：[mvp-scope.md](mvp-scope.md)（做什么）。本文回答怎么做：架构、包结构、核心机制、约定。
> 原则：设计对齐原型（JChat）中已被验证的模式，代码全部原创；原型的已知缺陷一律修复（标注 🩹）。

## 1. 架构总览

```
┌────────────────────────────────────────────────────────────┐
│                前端 ui/（React 19 + AntD 6 + @ant-design/x） │
│   三视图：Agent 管理 / 会话列表 / 知识库管理  +  聊天主视图    │
└─────────────┬──────────────────────────────▲───────────────┘
              │ REST (21 端点)               │ SSE（4 事件）
┌─────────────▼──────────────────────────────┴───────────────┐
│  Controller(薄) → FacadeService(编排) → Mapper(MyBatis XML)  │
│                                                             │
│  ┌───────────────┐ ┌──────────────┐ ┌────────────────────┐ │
│  │ AgentLoop     │ │ RAG          │ │ SseService         │ │
│  │ Think-Execute │ │ Splitter接口 │ │ SseEmitter 池      │ │
│  │ 状态机+工具    │ │ EmbeddingClient│ │ 4 事件类型       │ │
│  └──────┬────────┘ └──────┬───────┘ └────────────────────┘ │
│         │  ChatEvent(@Async) │                               │
│  ┌──────▼──────────────────▼─────────────────────────────┐ │
│  │ ChatClientRegistry（Bean名=模型名：glm-4.6/deepseek-chat）│ │
│  └──────┬────────────────────────────────────────────────┘ │
│  ┌──────▼──────────────┐  ┌─────────────────────────────┐  │
│  │ PostgreSQL agenthub │  │ PostgreSQL eshop（第二数据源）│  │
│  │ + pgvector 1024维    │  │ 只读演示库，SqlQueryTool 用  │  │
│  └─────────────────────┘  └─────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
外部 API：智谱（glm-4.6 对话 / embedding-3 向量）、DeepSeek（deepseek-chat 对话）
```

## 2. 包结构（com.agentforge，Maven 单模块，Java 21）

```
com.agentforge
├── AgentForgeApplication.java
├── agent/                      # Agent 核心（灵魂模块）
│   ├── AgentLoop.java          # Think-Execute 循环（对应原型 JChatMind）
│   ├── AgentState.java         # IDLE/THINKING/EXECUTING/FINISHED/ERROR（真状态机，非摆设）
│   ├── AgentRuntimeFactory.java# 装配：加载配置→回放记忆→解析工具/知识库→选模型 🩹配置全部局部变量
│   └── tools/                  # AgentTool 接口 + ToolType(FIXED/OPTIONAL) + 4 实现
│       ├── AgentTool.java / ToolType.java   # 命名避开 Spring AI @Tool 注解类型撞名
│       ├── TerminateTool.java      # FIXED 信号工具（空方法体，主循环识别响应名）
│       ├── KnowledgeQueryTool.java # FIXED，调 RagService.similaritySearch（M6）
│       ├── CurrentTimeTool.java    # OPTIONAL（M5）
│       └── SqlQueryTool.java       # OPTIONAL，只读 SELECT，走 eshop 数据源（M5）
├── rag/
│   ├── Splitter.java           # 接口：List<Chunk> split(String rawMarkdown) —— 拓展缝
│   ├── MarkdownTitleSplitter.java  # 实现：flexmark 按标题章节切（含表格保留原文）
│   └── ZhipuEmbeddingClient.java   # RestClient 调智谱 embedding-3(dimensions=1024)，带重试
├── config/
│   ├── ChatClientRegistry.java     # Map<String,ChatClient> 注入
│   ├── MultiChatClientConfig.java  # @Bean("glm-4.6") @Bean("deepseek-chat")
│   ├── DataSourceConfig.java       # 主数据源(agenthub) + eshop JdbcTemplate
│   ├── AsyncConfig.java            # agent-executor 线程池 core4/max10/queue100
│   └── CorsConfig.java
├── controller/                 # 8 个薄 Controller（21 端点，见 api-contract.md）
├── service/ + service/impl/    # FacadeService 编排层（11 个接口）
├── mapper/                     # 6 Mapper + resources/mapper/*.xml
├── converter/                  # 6 个 Converter（Jackson 手写，不用 MapStruct）
├── model/
│   ├── common/ApiResponse.java
│   ├── entity/ dto/ vo/ request/ response/
├── sse/                        # SseService + SseMessage（4 事件类型）
├── event/                      # ChatEvent + ChatEventListener(@Async)
├── typehandler/PgVectorTypeHandler.java  # float[] ↔ vector 文本协议
└── exception/                  # BizException + GlobalExceptionHandler
```

## 3. 核心机制设计

### 3.1 Agent 循环（六路径，与 M4 测试计划一一对应）

```
run():
  IDLE → for step in 1..MAX_STEPS(20):
    think():  system=决策提示词(注入可用KB列表), messages=chatMemory
              chatOptions.internalToolExecutionEnabled=false, toolCallbacks=装配结果
    ├─ 返回无 toolCalls → 持久化 assistant 消息 → 推 GENERATED_CONTENT → FINISHED   [路径1]
    └─ 返回有 toolCalls → 持久化(含 toolCalls 元数据) → 推 THINKING
      execute(): toolCallingManager.executeToolCalls(prompt, lastResponse)
        → chatMemory.clear() + conversationHistory() 整体回写  # 保证 tool_calls 与 tool 响应成对
        → 逐条持久化 ToolResponse(含元数据) → 推 EXECUTING(工具名) + GENERATED_CONTENT
        ├─ 响应含 terminate → FINISHED                                          [路径2]
        ├─ 未含 → 下一轮 think                                                   [路径3]
        ├─ 第二轮工具调用链（回归路径）                                            [路径4]
      step==20 → FINISHED + warn                                                 [路径5]
  任意异常 → ERROR → 推 DONE(带错误标记) → 消息已落库不丢失                        [路径6]
```

关键决策（与原型一致，已验证）：
- `internalToolExecutionEnabled(false)` —— 循环控制权在自己手里；
- terminate 为**信号工具**（方法体为空），终止逻辑在主循环识别响应名，工具本身无副作用；
- Agent 产出消息走**不发事件**的持久化重载（防事件风暴死循环）。

### 3.2 记忆持久化协议（对应 Spring AI #2101 痛点）

| DB role | DB content | metadata JSONB | 回放为 |
|---|---|---|---|
| user | 用户输入 | — | `UserMessage` |
| assistant | 文本(可空) | `toolCalls:[{id,type,name,arguments}]` | `AssistantMessage.builder().content().toolCalls()` |
| tool | 工具结果 | `toolResponse:{id,name,responseData}` | `ToolResponseMessage.builder().responses()` |
| system | 系统提示词 | — | 不落库，Factory 每次由 Agent.systemPrompt 重建 |

窗口：按 Agent 配置 messageLength（默认 10）取最近 N 条；DB 层 `ORDER BY created_at DESC LIMIT n` 后应用层反转。

### 3.3 SSE 事件协议（4 种，全部真实实现）

连接：`GET /sse/connect/{sessionId}` → `SseEmitter`（30min 超时，`ConcurrentHashMap<sessionId,emitter>`，onCompletion/onTimeout/onError 清理）。连接成功即发具名事件 `init: connected`。

业务事件（具名 `message`，data 为 SseMessage JSON）：

| type | 时机 | payload 要点 |
|---|---|---|
| `AI_THINKING` | think 返回 toolCalls 后 | statusText="正在思考: {工具名}" |
| `AI_EXECUTING` | 每个工具执行前 | statusText="正在执行: {工具名}" |
| `AI_GENERATED_CONTENT` | 每条消息落库后 | 完整 ChatMessageVO（含工具元数据） |
| `AI_DONE` | 循环结束（含异常） | done=true, statusText=错误摘要或空 |

🩹 修复原型缺陷：`send()` 找不到连接**不抛异常**（log.debug + 返回），消息已持久化，前端拉历史可补。

### 3.4 双数据源

- 主：`spring.datasource` → agenthub（MyBatis 全部 Mapper）；
- eshop：独立属性 `eshop.datasource.*` → 单独 `JdbcTemplate eshopJdbcTemplate`，仅供 SqlQueryTool；测试库 `eshop_test` 同构。
- 🩹 SqlQueryTool 安全：仅 `SELECT/WITH` 开头（trim+toUpperCase）、拒绝分号多语句、`LIMIT 200` 强制包裹、只读连接（用户无写权限）。

### 3.5 Embedding 客户端

`ZhipuEmbeddingClient`：POST `https://open.bigmodel.cn/api/paas/v4/embeddings`，body `{"model":"embedding-3","input":text,"dimensions":1024}`；RestClient + 3 次指数退避重试；批量分块入库时逐块调用（MVP 不做 batch，接口预留 `embedAll`）。

## 4. 配置与环境变量（密钥零明文）

```yaml
spring.datasource.url:      jdbc:postgresql://localhost:5432/agenthub
spring.datasource.username: ${PG_USER:postgres}
spring.datasource.password: ${PG_PASSWORD:123456}
eshop.datasource.url:       jdbc:postgresql://localhost:5432/eshop
spring.ai.zhipuai.api-key:  ${ZHIPU_API_KEY}
spring.ai.zhipuai.chat.options.model: glm-4.6
spring.ai.deepseek.api-key: ${DEEPSEEK_API_KEY}
spring.ai.deepseek.chat.options.model: deepseek-chat
```

embedding 的 key 复用 ZHIPU_API_KEY（同账号）。`.env.example` 提交模板，`.env` 进 .gitignore。

## 5. 全局约定

- 分层：Controller 不写业务；FacadeService 编排 + 事务边界；Mapper 只 SQL；Converter 是 Entity↔DTO↔VO 唯一通道。
- 命名：类名照第 2 节；REST 路径与 api-contract.md 逐字一致；SSE 事件名与 3.3 节逐字一致。
- 错误：业务错误抛 `BizException(msg)` → HTTP 200 + code=500；未知异常统一"服务器内部错误"；不存在资源返回真 404。
- 测试：单测 mock 一切外部（ChatClient/Embedding/DB）；集成测试 `@Tag("integration")` 连本地 PG 测试库；每个里程碑的测试清单见 mvp-scope.md §2。
- 提交：遵守 memory.md（改动即 commit；全量测试过才算完成）。
