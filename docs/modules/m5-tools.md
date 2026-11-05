# M5 模块讲解稿：工具体系（你先答，AI 补全）

> 对应代码：`agent/tools/SqlQueryTool.java`、`CurrentTimeTool.java`、`service/ToolFacadeService`、`controller/ToolController.java`。

## 问题清单

1. **SqlQueryTool 的四层护栏分别是什么、各防什么？哪层是"最后防线"、为什么？**
2. **为什么要用子查询包裹强制 LIMIT 200，而不是相信模型自己写 LIMIT？如果模型给的 SQL 里已经有 LIMIT 5，再包一层结果是什么？**
3. **"出现分号即拒绝"会误伤字符串常量里含分号的 SQL（如 `WHERE remark='a;b'`）——为什么仍然这么设计？这叫什么权衡？**
4. **GET /api/tools 为什么只返回 OPTIONAL 工具、不返回 FIXED？前端拿这个清单做什么？**
5. **SqlQueryTool 的 DESCRIPTION 里写了表结构和常用列名——这对待调工具的模型行为有什么影响？这种做法叫什么？**
6. **CurrentTimeTool 遇到非法格式串时回退默认格式而不是抛异常；SqlQueryTool 拒绝非法 SQL 时返回 JSON 错误而不是抛异常——工具错误处理的这条"哲学"是什么？为什么？**
7. **工具校验失败返回 `{"error":"..."}`，模型收到这个 JSON 后通常会发生什么？这个闭环为什么重要？**
8. **M4 的 Factory 单测用了假工具，为什么 M5 还要加 AgentToolAssemblyIT 用真实 Bean 再测一遍装配？两者各覆盖什么？**

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

- **1**：①语法白名单（仅 SELECT/WITH 开头）拦显式写操作；②分号拒绝拦多语句注入（SELECT 1; DROP TABLE）；③LIMIT 200 包裹兜住资源（模型可能写无 LIMIT 全表扫）；④数据源只读（Hikari readOnly + eshop_ro 无写权限）。**最后防线是④**——前三层都在应用代码里，理论上可能被绕过（如 WITH 里藏写法、注释欺骗），只有数据库权限是独立于应用代码的硬墙。纵深防御的要点：任何一层失守，下一层还在。
- **2**：模型不一定写 LIMIT（提示词约束不可靠），全表扫会拖慢库和 Agent。已有 `LIMIT 5` 再包裹：内层先取 5 行，外层 `LIMIT 200` 只是硬上限——`SELECT * FROM (…LIMIT 5) AS q LIMIT 200` 结果仍是 5 行，无害且幂等。
- **3**：**可用性换安全性（fail-closed）**。演示库场景几乎不会有字符串常量含分号的查询，而多语句注入是真实威胁；误伤的代价（模型换个写法重试）远小于被注入的代价。且分号在 PG 协议层本来就是多语句语义，拒绝是协议对齐而非过度设计。
- **4**：前端 Agent 表单只让用户勾 OPTIONAL；FIXED（terminate）是循环机制的一部分，用户勾不勾都存在——展示它反而制造"不勾就不终止"的错误心智。契约即产品语义。
- **5**：模型选工具靠**工具描述与参数描述**里的信息做决策（function calling 的原理：schema + 描述进 prompt）。把表结构写进描述=把"怎么用好这个工具"的领域知识喂给模型，显著提高首次调用命中率。实践名叫 **prompt engineering for tools / 工具描述工程**（好的工具描述是最小成本的准确率优化）。
- **6**：**工具的错误也是给模型的信息，不是异常**。抛异常会把 AgentLoop 打进 ERROR 终止整轮（六路径的路径 6）；返回错误 JSON 则模型能读到原因、自己修正 SQL 重试——工具级失败被降级为循环内的一次正常交互。只有不可恢复的系统级错误才值得抛。
- **7**：模型读 `{"error":"仅支持 SELECT…"}` → 理解失败原因 → 改写 SQL 或放弃并直接回答。闭环价值：**自愈**——工具失败不终止任务，Agent 还有机会完成目标；这也是 ReAct 模式的本义（观察结果→调整行动）。
- **8**：M4 单测覆盖**装配逻辑**（FIXED/OPTIONAL 过滤、按名匹配——用假工具隔离验证规则本身）；AssemblyIT 覆盖**真实集成点**：Spring 容器里的真 @Component 工具 Bean（带 @Tool 注解、可能被 AOP 代理）能否真被 MethodToolCallbackProvider 扫出回调、名字对齐 allowedTools 字符串。前者防逻辑回归，后者防"真实 Bean 长得和假工具不一样"的装配事故（比如注解忘写 name、代理未解包）。
