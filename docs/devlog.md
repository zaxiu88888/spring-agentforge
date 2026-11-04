# 项目开发日志（devlog）

> 规则四：每干一步追加一条，只追加不改写。格式：日期 · 步骤 · 做了什么 / 关键决策 / 踩坑 / 测试结果 / commit。
> 粒度说明：日志按日期 + 步骤顺序记录（AI 辅助开发节奏快，不按小时计）；git 提交历史是唯一精确时间线。

---

## 2026-11-04

### 前置环境搭建（仓库初始化之前）
- 验证两组 API key：智谱（glm-4.6 对话 + embedding-3 指定 dimensions=1024）、DeepSeek（deepseek-chat）均真实调用成功。
- 调研 pgvector Windows 安装：官方无预编译包，MSVC 编译需管理员权限（本机无）。决策：**Miniforge conda 一键装 postgresql+pgvector 到 E:\PostgreSQL，免管理员**。
- 建库 agenthub（6 表，VECTOR(1024)+ivfflat），端到端向量检索验证：中文查英文文档 L2=0.32 / 无关文本 0.89，语义区分度正常。
- 踩坑：Git Bash(MSYS) 吞反斜杠路径（conda -p 参数），改正斜杠解决；JDK 坑见后。
- 产出：E:\PostgreSQL\{start,stop}-pg.bat；Docker Desktop 按用户决定暂缓（安装包已备）。

### 仓库初始化与工作规则 · `36e22e2`
- 项目定名 **AgentForge**（GitHub 名 spring-agentforge，查名零撞名）；git init -b main。
- memory.md：规则一（改动即 commit，原子 + `类型: 描述`）、规则二（测试全过才能说做完）。

### 竞品调研 prior-art · `6191e43`
- GitHub API 抓 5 项目（Spring AI / Spring AI Alibaba / LangChain4j / Dify / LobeChat）元数据 + 75 条高赞 issues + README。
- 关键结论：Spring AI #2101 工具调用不进记忆是官方痛点（我们的持久化协议是差异化点）；分块策略（#5464/#1081）与可观测性（#2331）是拓展方向；MVP 骨架收敛为 8 里程碑。
- 教训：3 个并行子代理撞模型并发上限全灭，改 curl/python 直连 GitHub API 自己做，更快更准。

### MVP 范围定稿 · `76b5005`
- 用户三决策：前端完整三视图 / SQL 工具查 eshop 演示库 / 集成测试连本地 PG（不用 Testcontainers）。
- API 面定稿 21 端点；砍：邮件工具、消息级编辑删除、AI_PLANNING 事件、认证、MCP 等。

### 开工前文档四件套 · `2d57401`
- tech-design（架构/包结构/Agent 六路径/记忆协议/SSE 事件协议/双数据源/Embedding 客户端）。
- api-contract（21 端点 + SSE，前后端逐字对齐的 TS 类型）。
- setup（环境手册）+ README 门面。同时建仓库外交接文档 E:\jjchat\HANDOFF.md。

### 规则三自审门 · `dd1115c`
- 里程碑完成前 diff 四查（契约/并发/安全/覆盖）+ 汇报附自审清单；小改动豁免。

### M1 工程骨架 · `c1ef489` + `b6952bc`
- 后端：pom（Boot 3.5.8 + Spring AI 1.1.0 BOM + MyBatis/PG/flexmark）、密钥全环境变量（哑默认值保 context 加载）、ApiResponse/BizException/全局异常、/health。
- 前端：Vite8 + React19.3 + TS7 + antd6.7 + @ant-design/x2.10（比原型新一个大版本）。
- 测试：mvn test 6 全绿；tsc+vite build+oxlint 零错误。
- **踩坑：本机 JAVA_HOME 指向 JDK 11 报"不支持发行版本 21"，须 `export JAVA_HOME="/e/jdk 21"`**（已记入 setup.md）。
- 自审发现：mybatis 配置因包未建延后 M2（有意偏差）；key 哑默认值的代价记入 M3 待办（用前校验）。

### 远程仓库接入
- remote: https://github.com/zaxiu88888/spring-agentforge.git（凭据已缓存）；7 commits 推送验证通过。用户补齐 Topics/Description。

### M2-1 数据库脚本 · `81a5dad`
- db/schema.sql（幂等版业务 schema）+ db/eshop.sql（4 类目/10 商品/6 客户/10 订单/13 明细种子，手机数码卖最多供演示）+ init-all.bat（四库：agenthub(_test)/eshop(_test)）+ eshop_ro 只读角色。
- 验证：eshop_ro SELECT 10 行 ✅、UPDATE permission denied ✅。
- **踩坑：bat 里中文注释被 cmd 按 GBK 解析直接炸，改纯 ASCII**。

### M2-2~4 数据访问层 · `7dee6a3`
- 双数据源 DataSourceConfig（显式装配主库 + eshop 只读 Hikari）、CorsConfig、PgVectorTypeHandler（float[] ↔ "[..]" 协议）、6 套 entity/dto/vo/request/response/converter、6 Mapper + XML（UUID CAST / JSONB ::text 读 CAST 写 / VECTOR ::vector）。
- 设计要点：ChatMessage 元数据直接复用 Spring AI 的 ToolCall/ToolResponse record 做 JSON 序列化（M4 记忆协议同源）。

### M2-5~6 API 层与集成测试 · `3ccf95d`
- 17 端点 Service+Controller（upload 占位到 M6）；测试 yaml 指向 *_test 库。
- **踩坑 1：Surefire 默认不跑 `*IT.java`**（那是 Failsafe 命名约定），首跑 10 个测试里 5 个 IT 全静默跳过——pom 加 includes 让 `mvn test` 一条命令全覆盖。
- **踩坑 2（真 bug）：双 DataSourceProperties Bean 按类型注入被 @Primary 抢占，eshop 数据源实际连到 agenthub_test**（psql 通、JDBC 不通，加"连接身份断言"测试后现形）。修复：全部 @Qualifier 按名装配。
- **踩坑 3**：Hikari readOnly 先于 DB 角色拦截写操作，异常消息不含 "permission denied"——测试改为分层验证两层保护。
- 最终：**Tests run: 20, Failures: 0, Errors: 0 BUILD SUCCESS**（6 个 IT：Agent CRUD 全链路/级联/元数据回放/向量检索+KB 隔离/eshop 双保险/KB 批量）。
- 自审：契约逐字一致（1 处计划内偏差：upload 占位）；全 #{} 零 ${}；缺口记录——Session/KB Controller 未单独 HTTP 测试、CorsConfig 待 M8。

### 规则四：项目日志 + 交接同步（本条所在 commit）
- 补建本 devlog（回填至今全部步骤）；memory.md 增规则四与核对单更新；HANDOFF.md 每步同步。

### devlog 粒度调整 + 模块讲解稿计划
- 用户提出"做得太快会引人怀疑"：决策——①日志去时钟精度只留日期（日期真实不变，非改写历史）；②**不做**任何 git 历史改期/伪造多天记录（经不起追问且无必要，AI 辅助开发是常态，能讲透才是关键）；③ M3 起放慢节奏，每里程碑配"模块讲解稿"（深挖问答，面试弹药库），用户先答、AI 补全。

### M3 多模型注册表
- MultiChatClientConfig：`@Bean("glm-4.6")`/`@Bean("deepseek-chat")` 两个 ChatClient；**Bean 名=注册 key=ModelType 枚举值三方对齐**；@PostConstruct 哑 key 启动告警（清偿 M1 自审欠账）。
- ChatClientRegistry：Map 注入 + Map.copyOf 不可变 + get/require 双语义（require 抛 BizException 消息直达前端）。
- 测试：纯单测 3（mock ChatClient 验注册表语义）+ 装配 IT 2（双模型存在 + 遍历 ModelType 断言每个枚举值都有同名 Bean——三方对齐不变量守护）。**Tests run: 25, Failures: 0 BUILD SUCCESS**。
- 首个模块讲解稿 docs/modules/m3-multi-model-registry.md（8 问，用户先答 AI 补全）。
- 自审：契约一致（枚举值与 api-contract ModelType 逐字同）；Registry 不可变无并发问题；哑 key 风险已加告警；覆盖达 mvp-scope M3 要求。
