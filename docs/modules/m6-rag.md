# M6 模块讲解稿：RAG 链路（你先答，AI 补全）

> 对应代码：`rag/Splitter.java`、`MarkdownTitleSplitter.java`、`ZhipuEmbeddingClient.java`、`service/impl/RagServiceImpl.java`、`DocumentFacadeServiceImpl`（上传）、`agent/tools/KnowledgeQueryTool.java`。

## 问题清单

1. **完整描述一次 md 文档从上传到可被检索的全流程（六步），每步产出什么？**
2. **为什么按 Markdown 标题分块，而不是像主流框架那样固定 token 长度切块？我们用标题文本做 embedding，理由和局限分别是什么？**
3. **Splitter 为什么定义成接口？将来要加"固定 token 滑窗"策略，需要改哪些代码、不用改哪些？**
4. **向量化中途失败时，为什么选择整体回滚（删文档记录 + 删文件）而不是保留文档、跳过失败的分块？**
5. **embedding-3 请求里 dimensions=1024，为什么必须和表的 VECTOR(1024) 一致？不一致会发生什么？这个 1024 当初是怎么定的？**
6. **KnowledgeQueryTool 为什么是 FIXED（所有 Agent 必装）？think 提示词里注入的【可用知识库】列表和工具的 kbId 参数是什么关系？**
7. **pgvector 我们用的是 L2 距离（`<->`）排序。L2 / 内积 / 余弦三种怎么选？什么信号出现时你会换成余弦（`<=>`）？**
8. **上传集成测试里你用 @MockitoBean 替换的是 RagService 整层，而不是只 mock ZhipuEmbeddingClient——这个替换层级的选择依据是什么？**

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

---

## 参考要点（答题后再看）

- **1**：①校验（KB 存在/文件非空/.md 后缀）→ ②建 document 记录拿 docId（先有 ID 才有落盘目录）→ ③文件落盘 `{base}/{kbId}/{docId}/{uuid}.md` 并回写 metadata.filePath → ④flexmark 解析，按顶层标题偏移量切片原文（表格保原始 Markdown）→ ⑤每块用标题文本调 embedding-3(1024) 得向量 → ⑥连 metadata(chunkIndex/title) 插入 chunk_embedding。检索侧：Agent 调 knowledgeQuery(kbId, query) → embed(query) → `ORDER BY embedding <-> query LIMIT 3` → top3 的 title+content 以 JSON 回给模型。
- **2**：按标题切=**语义完整单元**（一个章节讲一件事），检索命中后给模型的片段自带上下文，且无需处理"句子被拦腰切断"。局限：①章节长短极不均匀（长章节信息被单个向量稀释，短标题无信息量）；②无标题文档退化成整篇一块；③标题 embedding 只代表"这段讲什么主题"，细节问题可能召不准（改进方向：标题+首段拼接送入 embedding，或块内再滑窗二级检索）。固定 token 切的优点是均匀可控，缺点是切断语义——这就是 prior-art 里 #5464/#1081（官方语义分块、更多分块策略）被高赞的原因。
- **3**：策略可替换（策略模式）：接口只承诺"输入原文、输出分块序列"。加滑窗=新增一个 `TokenSlidingWindowSplitter implements Splitter` + 一处装配选择（按 KB 配置注入），上传/入库/检索全链路零改动——因为 DocumentFacadeServiceImpl 依赖的是 Splitter 接口而非具体类。不用改的：Document/Chunk 表、Mapper、RagService、KnowledgeQueryTool。
- **4**：**半入库的知识库比失败更糟**——文档在列表里可见，检索却静默漏数据（用户以为"知识库里没有"，实际是分块缺失），这种错误不可观测且误导诊断方向。回滚保证状态二值：要么完整可用，要么干净不存在；失败信息通过异常如实传达给上传者。
- **5**：pgvector 的 VECTOR(1024) 是列类型约束，维度不匹配的插入直接报错；即便建成无维度约束的 VECTOR，检索时 `<->` 也要求两边同维。1024 的来源：智谱 embedding-3 支持 dimensions 参数（默认 2560），1024 既对齐原型 BGE-M3 的维度习惯，又低于 ivfflat 索引 2000 维上限（再高只能换 hnsw 半精度），是"够用 + 索引友好"的取值。
- **6**：FIXED 的理由：知识检索是平台基础能力，模型"不知道自己不知道"——不预装，它面对私有知识问题会自信编造；think 提示注入后模型知道"有这些库可用"，才会主动查。关系：【可用知识库】列表（name + kbId）告诉模型**有哪些库**，kbId 参数是模型从列表里**选库**的凭证——没有提示注入，模型只能瞎猜 ID；没有工具参数，提示只是摆设。两者构成"导航 + 执行"闭环。
- **7**：L2 衡量绝对距离（受向量模长影响），内积不归一，余弦只看方向。embedding 模型输出通常已近似归一，三者结果高度相关，L2 是 pgvector 最常用默认。**切换信号**：当我们发现不同长度文本的 embedding 模长差异带来排序偏差（长文本模长偏小被系统性偏好/惩罚），或换用的 embedding 模型文档明确建议余弦时 → 建余弦索引（`vector_cosine_ops`）+ `<=>` 操作符。这是"先跑通再调优"的显式取舍。
- **8**：mock 的层级要对准**被测流程的边界**：上传 IT 要验证的是"上传→分块→入库→落盘"的完整内部链路，唯一的外部依赖是向量化 API——mock RagService 恰好切在这条边界上（embed 是唯一出口）。若只 mock ZhipuEmbeddingClient，则 RagService 实现也在测（它只是转发，无独立价值）；若连 Splitter/DB 都 mock，就不再是集成测试。原则：**mock 外部世界，保留自己的全部代码**。
