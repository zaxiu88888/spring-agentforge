# M3 模块讲解稿：多模型注册表（你先答，AI 补全）

> 用法：自己先口头/书面回答每题，再对照代码与「参考要点」修正。这是你的面试弹药，不是文档装饰。
> 对应代码：`config/MultiChatClientConfig.java`、`config/ChatClientRegistry.java`、`AgentDTO.ModelType`。

## 问题清单

1. **为什么需要 ChatClientRegistry？直接在业务里 if/else 判断模型名不行吗？**
2. **Registry 的构造函数只写了一个 `Map<String, ChatClient>` 参数，为什么没写任何注册代码，两个模型就进来了？这背后是 Spring 的什么机制？**
3. **Bean 名 `glm-4.6` 带着一个点，作为 Java 方法名不行、但作为 Bean 名可以——Bean 命名和 Java 命名分别受什么约束？我们为什么要让 Bean 名 = Agent.model 的值 = ModelType 枚举值三方对齐？**
4. **现在要接入第三个模型（比如 qwen-max），最少要改几处代码？分别在哪？如果用 if/else 方案，改动量是什么样？**
5. **`require()` 和 `get()` 为什么分成两个方法？抛 BizException 而不是 IllegalArgumentException，对前端意味着什么？**
6. **ChatClientRegistry 是单例 Bean，多个用户同时发消息、同时 `require()`，有没有并发问题？为什么？**
7. **application.yaml 里 api-key 有个哑默认值 `dummy-key-context-load`，它解决什么问题、引入什么风险？我们用什么手段降低这个风险？**
8. **`ChatClientRegistryWiringIT` 里那个"遍历 ModelType 枚举断言每个值都有同名 Bean"的测试，防的是什么事故？举一个它会拦下的真实失误。**

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

- **1**：if/else 把"模型选择"硬编码进业务，每加模型都要改业务代码（违反开闭原则），且无法在启动时校验配置合法性。注册表把"有哪些模型"与"用哪个模型"解耦：前者由 Spring 容器声明（@Bean），后者由数据驱动（Agent.model 字段）。
- **2**：Spring 的集合注入特性——当构造参数是 `Map<String, T>` 时，容器会把**所有 T 类型的 Bean** 注入，key 就是 Bean 名。所以"注册"这个动作被 @Bean 定义本身完成了，Registry 只是收敛读取入口。
- **3**：Java 标识符不能含 `.`，但 Spring Bean 名只是字符串。三方对齐后，新增模型零字符串散落——任何一处改名都会被 WiringIT 测试当场拦红（这就是问题 8 的事故：改了枚举值忘了改 Bean 名）。
- **4**：两处——MultiChatClientConfig 加一个 `@Bean("qwen-max")`（+对应 starter 依赖），ModelType 加一项枚举。业务代码、Registry、数据库全都不用动。if/else 方案则要改所有分支点，且容易漏。
- **5**：get 语义宽松（可空，适合探测/列表场景）；require 语义严格（Agent 装配时模型必须存在）。BizException 会被全局异常处理器转成 `ApiResponse{code:500, message}` 且 HTTP 200——错误消息能直达前端弹 toast；IllegalArgumentException 会走兜底"服务器内部错误"，用户看不到原因。
- **6**：没有。Registry 无可变状态（构造时 `Map.copyOf` 冻结，只读）；ChatClient 本身线程安全（无状态 Fluent API，每次调用创建新 prompt）。
- **7**：哑默认值让 `@SpringBootTest` 上下文能在没配 key 的环境（CI、别人 clone）里正常装配加载；风险是"忘了配 key 直到真正调模型才失败"。降低手段：`@PostConstruct` 启动时扫描到 `dummy` 前缀就 `log.warn` 明示哪个环境变量没配。
