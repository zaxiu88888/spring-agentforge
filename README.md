# AgentForge —— AI 智能体助手

基于 **Spring AI** 从零构建的 AI Agent 系统：Think-Execute 自主决策循环 + 工具调用 + RAG 知识库 + SSE 过程实时可视化 + 多模型注册表。

> 它不是聊天机器人，而是 Agent：能规划、能自主调用工具、能检索私有知识库、能把执行过程实时推给前端、重启后记忆完整恢复。

## 核心特性

- ✅ **Think-Execute 循环**：手写 ReAct 状态机（`internalToolExecutionEnabled(false)` + 手动 ToolCallingManager），最大 20 步，terminate 信号工具主动终止
- ✅ **工具框架**：FIXED / OPTIONAL 两类工具按 Agent 配置动态装配（SQL 查询 / 知识库检索 / 时间等），`MethodToolCallbackProvider` 扫描装配
- ✅ **RAG**：Markdown 按标题分块（Splitter 接口化，可扩展）→ 智谱 embedding-3(1024 维) → pgvector 向量检索 → 结果注入工具上下文
- ✅ **多模型注册表**：ChatClientRegistry 统一管理 GLM-4.6 / DeepSeek，Agent 级运行时切换，新增模型仅需一个 Bean
- ✅ **SSE 过程可视化**：THINKING / EXECUTING / GENERATED_CONTENT / DONE 四类事件全链路推送，前端展示思考气泡与工具调用卡片
- ✅ **消息持久化协议**：工具调用/结果随消息元数据入库，会话记忆按角色完整回放（解决 Spring AI 已知痛点 #2101）
- ✅ **双数据源**：业务库 + eshop 只读演示库，SQL 工具带只读白名单防护

## 技术栈

Spring Boot 3.5 · Spring AI 1.1 · MyBatis · PostgreSQL + pgvector · React 19 · TypeScript · Ant Design 6 · @ant-design/x · Vite

## 快速开始

见 [docs/setup.md](docs/setup.md)（数据库初始化 / 环境变量 / 启动命令）。

```bash
mvn spring-boot:run      # 后端 :8080
cd ui && npm run dev     # 前端 :5173
```

## 文档导航

| 文档 | 内容 |
|---|---|
| [docs/prior-art.md](docs/prior-art.md) | 竞品调研（为什么这样做） |
| [docs/mvp-scope.md](docs/mvp-scope.md) | MVP 范围定稿（做什么/不做什么） |
| [docs/tech-design.md](docs/tech-design.md) | 技术设计（架构/包结构/核心机制） |
| [docs/api-contract.md](docs/api-contract.md) | API 契约（21 端点 + SSE 协议） |
| [docs/setup.md](docs/setup.md) | 环境手册 |
| [memory.md](memory.md) | 工作纪律（改动即提交；测试全过才算完成） |

## 项目结构（随里程碑更新）

```
agentforge/
├── docs/          # 定稿文档（开工前锁定）
├── db/            # 建库脚本（M2）
├── src/main/java/com/agentforge/   # 后端（M1 起）
└── ui/            # 前端（M1 起）
```
