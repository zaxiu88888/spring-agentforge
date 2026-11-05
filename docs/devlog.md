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

## 2026-11-05

### M4 Agent 循环（核心中的核心）
- 新增：AgentState 真状态机（IDLE/THINKING/EXECUTING/FINISHED/ERROR 实流转）、AgentLoop（六路径，一次性实例不可重入）、AgentMessageSink、AgentProgressListener（M4 日志实现/M7 换 SSE）、AgentRuntimeFactory（🩹修复原型共享状态并发缺陷：装配中间态全局部变量）、AgentTool/ToolType/TerminateTool、ChatEvent + ChatEventListener(@Async("agentExecutor"))、AsyncConfig(4/10/100)、Service 增 getRecentMessages + createChatMessage 仅 USER 角色发事件。
- **踩坑 1（命名撞车）**：自定义 `Tool` 接口与 Spring AI 的 `@Tool` 注解类型（本身是接口）同名，import 相互遮蔽导致"注解未覆盖 resultConverter()"诡异报错——接口改名 **AgentTool** 彻底避撞，tech-design 已同步。
- **踩坑 2（API 差异）**：Spring AI 1.1 无 `ToolCallback.getName()`，用 `getToolDefinition().name()`；`AssistantMessage.builder().toolCalls(null)` 会抛异常（要传空列表）；`DefaultToolCallingChatOptions.builder().build()` 声明类型要写接口 ToolCallingChatOptions。
- **测试抓到真 bug**：execute 遍历全量 `conversationHistory()` 导致上一轮工具响应**重复落库**（maxSteps 测试的 5≠4 现形）——修复为按 `messagesBefore.size()` 界只持久化新增段。教训记入 M4 讲解稿第 7 题。
- 测试策略：ChatClient 深桩 mock 控制模型输出序列，ToolCallback/ToolCallingManager 全真——六路径 + 记忆成对相邻断言 + Factory 装配/异常 + Listener 兜底 + Service recent 正序。**Tests run: 41, Failures: 0 BUILD SUCCESS**。
- 设计决策（与原型的差异）：①循环异常吞掉不上抛（@Async 线程无人接，改为 onDone(错误) + ERROR 状态）；②最终直接回答不回写内存记忆（实例一次性，落库即可，下次运行 DB 回放）——均已写入测试注释。
- 自审四查：契约一致（仅 USER 发事件对齐 api-contract §3）；Factory 无状态/线程池有界；无新增 SQL 面；覆盖 mvp-scope M4 全部六路径。缺口记录：POST→事件→异步执行的端到端链路待 M8 用真 key 联调验证。

### M3 讲解稿作答批注
- 用户完成全部 8 题作答（8/8 命中，第 7/8 题用上"假启动/配置漂移"术语，评优秀）。批注与补强已回写 docs/modules/m3-multi-model-registry.md：第 3 题补"单一事实来源+测试守护"，第 5 题澄清"HTTP 200+业务 code"语义，第 7 题对照实际软校验实现并给出 prod fail-fast 升级话术。

### M4 讲解稿作答批注
- 用户完成全部 10 题作答。总评：概念方向感好（1/3/8/9 四题好），但对"自己代码的具体机制"掌握不牢——第 4 题答偏（记忆 clear+重建的真实原因是 conversationHistory 全量重建防消息序列非法，不是防膨胀）、第 5/7 题根因错（原型缺陷是 Factory 实例字段共享非 Memory 共享；重复落库是全量遍历非双重 add）。这三题恰是面试杀手锏，批注已逐一纠正回写 docs/modules/m4-agent-loop.md，建议用户针对性复述重练。

### git push 连接重置排查
- 现象：python/浏览器直连 github 正常，git push 报 "Recv failure: Connection was reset"。排查：无任何代理配置（系统/git/环境均无）→ 排除代理未传递；判定为 HTTP/2 多路复用通道被间歇重置。**解法：`git -c http.version=HTTP/1.1 push` 成功，已 `git config http.version HTTP/1.1` 固化到 repo-local**。

### M5 工具体系
- SqlQueryTool（OPTIONAL）：四层护栏——SELECT/WITH 白名单、分号多语句拒绝、LIMIT 200 子查询强制包裹、数据源只读（M2 的 Hikari readOnly + eshop_ro 双保险做最后防线）；返回结构化 JSON（columns/rowCount/rows 或 error），描述常量内嵌表结构（工具描述工程）。
- CurrentTimeTool（OPTIONAL）：默认格式 yyyy-MM-dd HH:mm:ss，非法格式回退默认不抛错（工具错误也是给模型的信息）。
- ToolFacadeService + GET /api/tools（ToolVO record，仅 OPTIONAL）；api-contract §6 对齐。
- **踩坑（编译主错引发雪崩）**：`@Tool(description = getDescription())` 注解值不允许方法调用，必须编译期常量；该编译错中断注解处理轮次，Lombok 全体失效，AgentLoop/Factory 冒出几十个"找不到符号"——先修第一个编译错再看后面的，多数是连带。
- 测试：护栏纯单测 6（白名单/多语句/包裹/JSON 错误）+ CurrentTime 3 + 真库 IT 4（含"哪类卖最好"聚合查询——验收场景原型）+ Controller 切片 1 + 真实 Bean 装配 IT 1。**Tests run: 55, Failures: 0 BUILD SUCCESS**。
- 自审四查：/api/tools 契约一致（data 直接数组）；JdbcTemplate 唯一 Bean 构造注入无歧义；护栏四层+错误信息不含库表结构泄露（仅回显截断 SQL）；覆盖 mvp-scope M5 测试要求（SQL 拦截/装配）。讲解稿 docs/modules/m5-tools.md（8 问待答）。

### M6 RAG 链路
- rag 包三件套：Splitter 接口（拓展缝）+ MarkdownTitleSplitter（flexmark 顶层标题偏移量切原文，表格保留原始 Markdown，前言为无标题块）+ ZhipuEmbeddingClient（embedding-3/dimensions=1024，3 次指数退避，哑/空 key 使用点 fail-fast）。
- RagService（embed + similaritySearch→pgvector L2 top3）；DocumentStorageService（{base}/{kbId}/{docId}/{uuid}.md，删空父目录）；DocumentFacadeServiceImpl 上传真实现：校验→建记录→落盘→分块→逐块向量化入库→回写 filePath，**向量化失败整体回滚（半入库的知识库比失败更糟）**；删除连带清文件。
- KnowledgeQueryTool（FIXED）返回 title+content JSON；AgentLoop 增 knowledgeHint（Factory 解析 allowed_kbs → "name(kbId=...)" 注入 think 提示词，与工具 kbId 参数构成导航+执行闭环）。
- **踩坑**：空 key "" 不以 dummy 开头穿透守卫，单测意外发起真实 HTTP 得 401——守卫补 isBlank（测试守卫的价值：防真实外呼）；装配 IT 预期过时（knowledgeQuery 成为 FIXED）。
- 测试：分块器 5（标题/前言/表格原文/无标题/空）+ 哑 key 2 + KnowledgeTool 3 + 上传 IT 3（@MockitoBean RagService：上传→记录+filePath+3 块+磁盘文件；删除全清；失败回滚）+ Factory 提示串 1。**Tests run: 69, Failures: 0 BUILD SUCCESS**。
- 自审四查：契约一致（upload multipart/.md 限制/错误语义对齐 §5）；上传链路无并发共享态、失败回滚无残留；路径拼接全部经 normalize（无穿越面）、embedding key 不落日志；覆盖 mvp-scope M6（分块单测+真实向量入库召回）。遗留：真实 embedding API 的端到端（真 key 上传→检索）留 M8 验收联调。讲解稿 docs/modules/m6-rag.md（8 问待答）。

### M5 讲解稿作答批注 + M4 三题二次复述
- M5 八题 8/8 通过，术语运用显著进步（Schema 注入 / Fail Soft / Self-Correction / fail-closed）。
- **M4 第 4/5/7 题二次复述仍未通过**：仍在推测而非对照代码（第 4 题甚至脑补"conversationHistory 是清洗后的干净历史"，恰与事实相反——它是含全部中间态的全量快照）。批注升级为**代码级数据流走查**（三段消息序列实例）回写讲解稿，要求先读 AgentLoop.java 的 think()/execute() 再做第三次复述。教学观察：概念题答得好（M3/M5 全过），涉及自家代码机制时易脑补——后续里程碑讲解稿答题前先指认源码位置。
