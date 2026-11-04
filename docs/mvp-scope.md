# MVP 范围定稿（写代码前的框架契约）

> 2026-11-04 与用户对齐后锁定。变更需先改本文档再改代码。
> 前置调研见 [prior-art.md](prior-art.md)；工作纪律见仓库根 [memory.md](../memory.md)。

## 0. 一句话定调

**一个能自主调用工具、检索私有知识库、执行过程实时可见、记忆可恢复的 Agent 平台。**
MVP 的每个模块必须服务这条演示闭环；闭环之外一律不做。

## 1. 已拍板的三个决策

| 决策点 | 结论 | 影响 |
|---|---|---|
| 前端深度 | **完整三视图**（Agent / 会话 / 知识库管理 + 聊天视图） | PATCH 编辑接口回归；前端工作量约 2-3 倍于精简版 |
| SQL 工具 | **eshop 电商演示库**（独立第二数据源） | 引入多数据源配置（简历亮点）；需建 eshop 库 + 种子数据 |
| 测试策略 | **连本地 PG 的集成测试**（不用 Testcontainers，Docker 暂缓） | 建 `agenthub_test` / `eshop_test` 两个测试库，`@Tag("integration")` 分离 |

## 2. MVP 必做骨架（8 个里程碑，顺序即依赖序）

| 里程碑 | 内容 | 测试要求 |
|---|---|---|
| M1 工程骨架 | Maven 单模块（Java 21）+ Boot 3.5 + Spring AI 1.1 BOM；密钥全部环境变量注入；React 19 + Vite + AntD 6 + @ant-design/x 脚手架；`.gitattributes` / `.gitignore`；`ApiResponse<T>` + 全局异常 | 启动冒烟测试 + 上下文加载测试 |
| M2 数据与 CRUD | 6 张表 Mapper/XML（UUID/JSONB/VECTOR 三类映射）→ FacadeService → Controller；**eshop 第二数据源 + 种子数据**；PgVectorTypeHandler | Mapper/Service 集成测试（连本地 PG 测试库） |
| M3 多模型注册表 | `ChatClientRegistry`（Map 注入，Bean 名 = key = Agent.model 值）；glm-4.6 + deepseek-chat | 注册表单测（双模型 Bean 存在性、按名取用） |
| M4 Agent 循环 | Think-Execute 状态机；`internalToolExecutionEnabled(false)` + 手动 ToolCallingManager；记忆重建（clear + conversationHistory 回写）；terminate 信号工具；MAX_STEPS=20；**修复原型 Factory 共享状态并发缺陷**（配置改局部变量传参） | **循环状态机单测（mock ChatClient）：直接回答/工具一轮/工具两轮/terminate/超步数/异常 六路径** |
| M5 工具体系 | `Tool` 接口 + FIXED/OPTIONAL；首批 4 个：terminate(FIXED)、knowledgeQuery(FIXED)、getCurrentTime(OPTIONAL)、sqlQuery(OPTIONAL，只读 SELECT 白名单，走 esop 数据源) | 工具单测（SQL 拦截写操作、路径校验）+ 装配单测（FIXED 全量/OPTIONAL 按名） |
| M6 RAG 链路 | md 上传 → flexmark 标题分块（**Splitter 接口化，留拓展缝**）→ 智谱 embedding-3(1024) → pgvector L2 top-k → KnowledgeTool 注入 | 分块器单测（标题/表格/无标题文档）+ 检索集成测试（真实向量入库召回） |
| M7 SSE 推送 | 连接管理 + 4 种事件**真实**实现（THINKING/EXECUTING/GENERATED_CONTENT/DONE）；**修原型「未连接抛异常」缺陷**（无连接则跳过，消息已落库） | SseService 单测（无连接不抛错、事件序列） |
| M8 前端三视图 | 侧栏三 Tab + 聊天主视图（SSE 状态气泡 + 工具调用卡片 + XMarkdown 渲染 + 自动滚动）；Agent 弹窗**单页表单**（不做原型 522 行四子菜单弹窗）；KB 详情 + md 上传；eshop 演示话术 | 前端 build 通过 + `tsc` 零错误（无 JS 测试框架，规则二以构建校验替代） |

## 3. API 面（定稿 21 个端点）

| 模块 | 端点 | 说明 |
|---|---|---|
| Agent | `GET/POST /api/agents`、`DELETE/PATCH /api/agents/{id}` | 4 |
| 会话 | `GET /api/chat-sessions`、`GET .../agent/{agentId}`、`POST`、`DELETE/PATCH /api/chat-sessions/{id}` | 5（砍 GET by id） |
| 消息 | `GET /api/chat-messages/session/{sessionId}`、`POST /api/chat-messages`（Agent 触发入口） | 2（**砍消息级编辑/删除**） |
| 知识库 | `GET/POST /api/knowledge-bases`、`DELETE/PATCH .../{id}` | 4 |
| 文档 | `GET /api/documents/kb/{kbId}`、`POST /api/documents/upload`、`DELETE /api/documents/{id}` | 3（砍 GET 全量、POST 仅建记录） |
| 工具 | `GET /api/tools` | 1 |
| SSE | `GET /sse/connect/{chatSessionId}` | 1 |
| 健康 | `GET /health` | 1 |

统一返回 `ApiResponse{code,message,data}`；业务异常 HTTP 200 + code=500；真 404 交给 NoResourceFoundException。

## 4. 明确不做（防 scope creep，写死）

- 邮件工具（SMTP 授权码配置摩擦，演示价值低）
- `AI_PLANNING` 事件（循环无独立规划阶段）
- 认证 / 多用户 / 权限
- 混合检索、重排序、多分块策略实现（**只留 Splitter 接口缝**）
- 可观测性入库（日志先行）
- Docker 部署链、MCP、多 Agent 编排、语音、多模态、插件市场
- 消息级 PATCH/DELETE、GET 全量文档、GET session by id

## 5. 测试基线（规则二的落地方式）

- **单元测试**（默认执行）：Agent 循环状态机（mock ChatClient 六路径）、Splitter、Converter、ChatClientRegistry、SseService、工具安全拦截。
- **集成测试**（`@Tag("integration")`，需本地 PG 运行，测试库 `agenthub_test`/`eshop_test`）：Mapper CRUD、JSONB/VECTOR 映射、相似度检索 SQL、第二数据源路由。
- **前端**：`tsc -b` + `vite build` 零错误为完成线（不引入 JS 测试框架，避免脚手架膨胀）。
- 完成定义 = 新改动带测试 + `mvn test` 全绿 + 按规则一 commit。

## 6. 验收标准（MVP 完成的唯一定义）

1. 界面创建 Agent（选 glm-4.6 或 deepseek-chat、勾 sqlQuery/getCurrentTime、挂知识库）；
2. 上传 eshop 说明文档与业务 md → 提问“库里卖得最好的类目是什么” → **看到 SSE 推送：思考 → 调 sqlQuery → 工具结果卡片 → 最终回答**；
3. 提问知识库问题 → Agent 自主决定调 knowledgeQuery → 引用文档内容回答；
4. 刷新页面 → 会话与消息（含工具调用元数据）完整恢复，追问有上下文；
5. `mvn test`（含 integration tag）全绿；`npm run build` 零错误。
