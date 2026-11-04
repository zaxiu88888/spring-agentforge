# API 契约（前后端唯一编码依据，21 端点 + SSE）

> 与 mvp-scope.md §3 端点清单一一对应。路径/字段名逐字执行，改动先改本文档。
> Base URL：`http://localhost:8080/api`（SSE 例外：`http://localhost:8080/sse/...`）

## 0. 通用结构

```jsonc
// ApiResponse<T>：所有 REST 端点统一包裹
{ "code": 200, "message": "success", "data": { } }
// code: 200 成功 / 500 业务或服务器错误（HTTP 状态码仍为 200）；404 为真 HTTP 404
```

```ts
// 核心类型（TS 定义，与后端 VO 字段逐字一致）
type ModelType = "glm-4.6" | "deepseek-chat";
interface ChatOptions { temperature?: number; topP?: number; messageLength?: number; } // 默认 0.7 / 1.0 / 10
interface ToolVO { name: string; description: string; type: "FIXED" | "OPTIONAL"; }
interface AgentVO {
  id: string; name: string; description?: string; systemPrompt?: string;
  model: ModelType; allowedTools?: string[]; allowedKbs?: string[]; chatOptions?: ChatOptions;
}
interface ChatSessionVO { id: string; agentId: string | null; title?: string; updatedAt?: string; }
interface ToolCall { id: string; type: string; name: string; arguments: string; }   // arguments 为 JSON 字符串
interface ToolResponse { id: string; name: string; responseData: string; }
interface ChatMessageVO {
  id: string; sessionId: string; role: "user" | "assistant" | "system" | "tool";
  content: string; metadata?: { toolCalls?: ToolCall[]; toolResponse?: ToolResponse };
}
interface KnowledgeBaseVO { id: string; name: string; description?: string; }
interface DocumentVO { id: string; kbId: string; filename: string; filetype?: string; size?: number; }
```

## 1. Agent（4）

| 方法路径 | 请求 | data 响应 |
|---|---|---|
| `GET /agents` | — | `{ "agents": AgentVO[] }` |
| `POST /agents` | `CreateAgentRequest` | `{ "agentId": "uuid" }` |
| `DELETE /agents/{agentId}` | — | `null` |
| `PATCH /agents/{agentId}` | `UpdateAgentRequest`（全字段可空，null 不覆盖） | `null` |

```jsonc
// CreateAgentRequest
{ "name": "数据助手", "description": "查库+知识库",
  "systemPrompt": "你是一个严谨的数据助手…",
  "model": "glm-4.6",
  "allowedTools": ["sqlQuery", "getCurrentTime"],
  "allowedKbs": ["<kb-uuid>"],
  "chatOptions": { "temperature": 0.7, "topP": 1.0, "messageLength": 10 } }
// UpdateAgentRequest：同 Create，全部字段可省略
```

## 2. 会话（5）

| 方法路径 | 请求 | data 响应 |
|---|---|---|
| `GET /chat-sessions` | — | `{ "chatSessions": ChatSessionVO[] }`（updated_at DESC） |
| `GET /chat-sessions/agent/{agentId}` | — | `{ "chatSessions": ChatSessionVO[] }` |
| `POST /chat-sessions` | `{ "agentId": "uuid", "title?": "…" }` | `{ "chatSessionId": "uuid" }` |
| `DELETE /chat-sessions/{id}` | — | `null` |
| `PATCH /chat-sessions/{id}` | `{ "title": "…" }` | `null` |

## 3. 消息（2，Agent 触发入口在此）

| 方法路径 | 请求 | data 响应 |
|---|---|---|
| `GET /chat-messages/session/{sessionId}` | — | `{ "chatMessages": ChatMessageVO[] }`（created_at ASC） |
| `POST /chat-messages` | `CreateChatMessageRequest`（下） | `{ "chatMessageId": "uuid" }` |

```jsonc
// POST /chat-messages：落库 → 发布 ChatEvent(@Async 跑 Agent) → 立即返回。
// AI 回复不走 HTTP 响应，全部经 SSE 推送（§8）。
{ "agentId": "uuid", "sessionId": "uuid", "role": "user", "content": "库里卖得最好的类目？" }
```

## 4. 知识库（4）

| 方法路径 | 请求 | data 响应 |
|---|---|---|
| `GET /knowledge-bases` | — | `{ "knowledgeBases": KnowledgeBaseVO[] }` |
| `POST /knowledge-bases` | `{ "name": "产品手册", "description?": "…" }` | `{ "knowledgeBaseId": "uuid" }` |
| `DELETE /knowledge-bases/{id}` | — | `null`（级联删文档与分块） |
| `PATCH /knowledge-bases/{id}` | `{ "name?": "…", "description?": "…" }` | `null` |

## 5. 文档（3，RAG 入口）

| 方法路径 | 请求 | data 响应 |
|---|---|---|
| `GET /documents/kb/{kbId}` | — | `{ "documents": DocumentVO[] }` |
| `POST /documents/upload` | **multipart/form-data**：`kbId` + `file`（仅 .md） | `{ "documentId": "uuid" }` |
| `DELETE /documents/{id}` | — | `null`（连删文件与分块） |

上传语义：建 document 记录 → 落盘 `data/documents/{kbId}/{docId}/{uuid}.md` → MarkdownTitleSplitter 分块 → 逐块 embedding-3(1024) 入 `chunk_embedding`。解析失败不阻断上传（记录日志，文档标记失败）。

## 6. 工具（1）

| 方法路径 | data 响应 |
|---|---|
| `GET /tools` | `ToolVO[]`（仅 OPTIONAL，供 Agent 表单勾选） |

## 7. 健康（1）

`GET /health` → `"ok"`（裸字符串）。

## 8. SSE（1，聊天主视图的生命线）

- 连接：`GET /sse/connect/{chatSessionId}`（`text/event-stream`，无 /api 前缀）。建立即收具名事件 `init`，data=`"connected"`。
- 业务事件：具名 `message`，data 为下述 JSON。

```ts
interface SseMessage {
  type: "AI_THINKING" | "AI_EXECUTING" | "AI_GENERATED_CONTENT" | "AI_DONE";
  payload: { message?: ChatMessageVO; statusText: string; done: boolean };
  metadata: { chatMessageId?: string };
}
```

| type | 前端行为 |
|---|---|
| `AI_THINKING` | 状态气泡："✨ 思考中 · {statusText}" |
| `AI_EXECUTING` | 状态气泡："⚡ 执行中 · {statusText}"（含工具名） |
| `AI_GENERATED_CONTENT` | messages 数组**追加一条** ChatMessageVO（非增量拼接）；role=tool 渲染为工具结果卡片 |
| `AI_DONE` | 收起状态气泡；`statusText` 非空则 toast 错误摘要 |

前端约定：发消息（POST §3）后乐观插入用户气泡；AI 侧完全依赖 SSE；SSE 断线自动重连（EventSource 原生），重连后拉一次 `GET /chat-messages/session/{id}` 对账。

## 9. 错误响应示例

```jsonc
// 删除不存在的 Agent：
{ "code": 500, "message": "Agent 不存在: <id>", "data": null }
// 上传非 md 文件：
{ "code": 500, "message": "仅支持 Markdown 文档", "data": null }
```
