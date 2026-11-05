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

## 你的回答与 AI 批注（2026-11-05 完成作答）

**1. 为什么需要 Registry？**
> 用户：解耦，方便扩展。避免修改逻辑散落多处，模型选择集中管理，加模型时业务代码零改动。
>
> ✅ 正确。补强一点：还有**装配时 fail-fast**——Agent 配置了不存在的模型，装配瞬间就报错，而不是等到第一次调用才发现；且"用哪个模型"由数据（Agent.model 字段）驱动，不是代码分支驱动。

**2. 为什么没写注册代码两个模型就进来了？**
> 用户：Spring 的集合注入机制。自动收集容器中所有 ChatClient Bean，以 Bean 名为 key、实例为 value 组装注入。
>
> ✅ 完全正确。面试时可以再点一句："注册"这个动作被 @Bean 声明本身完成了，Registry 只是收敛读取入口——这是"容器即注册表"的思路。

**3. Bean 名带点 / 三方对齐？**
> 用户：Java 方法名受语法限制不能带点；Spring Bean 名仅受约定约束可带点（@Bean(name=...) 指定）。三方对齐保证 map.get() 查表准确。
>
> ✅ 正确但浅。更关键的收益是**字符串单一事实来源**：模型名字符串只在 Bean 定义和枚举各出现一次，任何一处改名都会被 WiringIT 当场拦红——你第 8 题自己就讲了这个机制，两题可以串起来答。

**4. 接第三个模型改几处？**
> 用户：最少 2 处——新增 1 个 @Bean、枚举加 1 项，业务代码 0 改动。if/else 需在每个路由点加 else if，改动量 O(N)。
>
> ✅ 正确。严格说还有第 3 处：pom 加对应 starter 依赖。答题时主动说"两处代码 + 一个依赖"更严谨。

**5. get/require 分离 + BizException？**
> 用户：get() 可返回 null 需处理；require() 找不到立刻抛异常。BizException 被全局异常拦截，前端拿到结构化业务错误码；IllegalArgumentException 容易被识别为 500 系统错误。
>
> ✅ 语义对，一处表述要精确：我们的错误模型是 **HTTP 200 + body 里 code=500**（业务码），不是 HTTP 500。IllegalArgumentException 的真实问题是走兜底 handler 变成"服务器内部错误"——原始消息丢了，前端不知道原因。这个细节 AgentApiIT 有断言，面试可引用。

**6. 并发问题？**
> 用户：没问题。Map 构造注入后只读，无共享可变状态，多线程并发读安全。
>
> ✅ 正确。再补一句更完整：构造里还做了 Map.copyOf 冻结；且 ChatClient 本身无状态（fluent API 每次调用新建请求），两层都安全。

**7. 哑默认值解决什么/引入什么风险？**
> 用户：解决非生产环境缺 Key 启动失败；风险是假 Key 进生产导致"假启动"，调用时才报错。降低手段：生产强制配置真 Key 无默认值，或启动时校验拦截。
>
> ⭐ 好，"假启动"这个词准确。对照实际实现：我们用的是 @PostConstruct 检测 dummy 前缀 **log.warn（软校验）**；你说的"启动拦截"可作为升级方案讲：`if (prod profile && dummy) throw`——软硬分级，答题时这样讲有层次。

**8. WiringIT 防什么事故？**
> 用户：防"枚举声明了但容器没对应 Bean"的配置漂移。例：重构误删 @Bean(name="glm-4.6") 的 name 属性，Bean 名变成方法名，测试在 CI 阶段拦截，避免线上运行时炸。
>
> ⭐ 优秀。"配置漂移"+具体事故例子正是面试官想听的。对称的反向事故也补一个：枚举值改名但 Bean 没改——测试同样拦。

**总评：8/8 命中，2 题优秀。** 补强方向：第 3 题往"单一事实来源+测试守护"上靠，第 5 题把"HTTP 200 + 业务 code"说精确。

---

## 参考要点（答题后再看）

- **1**：if/else 把"模型选择"硬编码进业务，每加模型都要改业务代码（违反开闭原则），且无法在启动时校验配置合法性。注册表把"有哪些模型"与"用哪个模型"解耦：前者由 Spring 容器声明（@Bean），后者由数据驱动（Agent.model 字段）。
- **2**：Spring 的集合注入特性——当构造参数是 `Map<String, T>` 时，容器会把**所有 T 类型的 Bean** 注入，key 就是 Bean 名。所以"注册"这个动作被 @Bean 定义本身完成了，Registry 只是收敛读取入口。
- **3**：Java 标识符不能含 `.`，但 Spring Bean 名只是字符串。三方对齐后，新增模型零字符串散落——任何一处改名都会被 WiringIT 测试当场拦红（这就是问题 8 的事故：改了枚举值忘了改 Bean 名）。
- **4**：两处——MultiChatClientConfig 加一个 `@Bean("qwen-max")`（+对应 starter 依赖），ModelType 加一项枚举。业务代码、Registry、数据库全都不用动。if/else 方案则要改所有分支点，且容易漏。
- **5**：get 语义宽松（可空，适合探测/列表场景）；require 语义严格（Agent 装配时模型必须存在）。BizException 会被全局异常处理器转成 `ApiResponse{code:500, message}` 且 HTTP 200——错误消息能直达前端弹 toast；IllegalArgumentException 会走兜底"服务器内部错误"，用户看不到原因。
- **6**：没有。Registry 无可变状态（构造时 `Map.copyOf` 冻结，只读）；ChatClient 本身线程安全（无状态 Fluent API，每次调用创建新 prompt）。
- **7**：哑默认值让 `@SpringBootTest` 上下文能在没配 key 的环境（CI、别人 clone）里正常装配加载；风险是"忘了配 key 直到真正调模型才失败"。降低手段：`@PostConstruct` 启动时扫描到 `dummy` 前缀就 `log.warn` 明示哪个环境变量没配。
