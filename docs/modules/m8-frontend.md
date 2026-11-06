# M8 模块讲解稿：前端三视图与 SSE 聊天（你先答，AI 补全）

> 对应代码：`ui/src/api/{http,api}.ts`、`components/{Sidebar,AgentModal,KbView,ChatView,MessageItem}.tsx`、`types.ts`。
> 本轮 GUI 验收抓到一个 UI bug（第 7 题）和若干测试工程坑（第 8 题），先读码再答。

## 问题清单

1. **EventSource 的 SSE 连接挂在哪个组件的哪个 effect 里？依赖数组是什么？为什么切会话不会串消息？**
2. **前端在哪两个时机"拉全量历史对账"？为什么需要对账——SSE 不是可靠传输吗？**
3. **乐观插入的用户消息 id 是 `local-${Date.now()}`，随后 AI_DONE 对账会把它替换掉。如果对账逻辑删掉，这个 local id 会造成什么问题？**
4. **THINKING/EXECUTING 状态气泡和消息列表是两个不同的 state（statusText vs messages）。为什么不让"思考中"也做成一条消息塞进列表？**
5. **AgentModal 用 `key={open ? (editing ? editing.id : "new") : "closed"}` 控制重挂载——这解决了什么问题？不用 key 会怎样（我们最初就踩了这个 lint 警告）？**
6. **工具结果卡片默认折叠只显示前 90 字符、点击展开 JSON。这个交互设计的动机是什么？`prettyJson` 为什么要 try-catch？**
7. **GUI 测试发现的 bug：发送后输入框不清空。根因是什么？（提示：AntD X 的 Sender 组件）修复为什么要同时加 value + onChange？**
8. **GUI 测试中 Playwright 的 click/check 系统性超时，最后靠 dom_cua 节点点击 + 坐标点击完成全部验收。这暴露了什么测试工程问题？如果一个团队要给这个前端写自动化回归，你建议怎么做？**

## 你的回答

（写在每题下方，AI 会批注补全。先读 ChatView.tsx / AgentModal.tsx / MessageItem.tsx）

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

- **1**：ChatView 的 `useEffect(..., [sessionId])`——连接与监听随会话切换销毁重建（return 里 es.close()）。不串消息的本质：**每个 ChatView 实例只服务一个会话**（App 里还给它加了 `key={sessionId}` 强制重挂载），SSE URL 绑定该会话 id，事件处理函数闭包引用的也是该会话的 setMessages。
- **2**：①AI_DONE 到达时；②EventSource onerror（断线自动重连后）。SSE 是 TCP 上的长连接，断线期间的**服务端事件会永久丢失**（EventSource 重连后不会补发）——SSE 是"尽力推送"不是"可靠投递"。落库的消息才是 source of truth，拉历史对账把"加速器"（SSE）与"事实"（DB）重新对齐。这套语义在后端 M7 就设计好了（未连接不抛错），前端这步是协议的另一半。
- **3**：对账时 `fresh.length >= prev.length ? fresh : prev` 会把含 local id 的列表整体换成服务端真数据；没有对账，local id 的消息永远留在列表里——同一条用户消息在"刷新前"（local id）与"刷新后"（真 id）key 不同，React 列表渲染层面还会引发 key 重复/闪烁类问题；若后续做"消息操作"（重发/删除）拿 local id 调接口必 404。
- **4**：数据与状态分离（M7 讲解稿第 7 题的后端版）。消息列表是**持久事实**（要追加、要恢复、要按 id 对账），状态气泡是**瞬态 UI 态**（来去无痕）。混进列表的问题：刷新恢复时历史里会混进"思考中"垃圾消息（它没落库）；对账时它会被整体替换掉，气泡闪没；语义污染——消息列表的每项都该有 id 和落库对应物。
- **5**：antd Modal 的表单初值只能在挂载时确定（useState 初始化器）。最初在 useEffect 里 setForm 重置，触发 react-hooks 的 set-state-in-effect 警告（React Compiler 拒绝优化该组件）。key 变化 → 组件销毁重挂 → useState 初始化器重新跑 → 表单天然归零。不用 key 的替代方案是把所有字段做成受控+defaultProps 派生，复杂度更高。这是"用重挂载换状态重置"的惯用法。
- **6**：工具结果常常是大 JSON（向量检索 3 段/SQL 200 行），默认全展开会把消息流撑爆、真正的回答被挤出视口——**信息密度优先级：结论 > 工具痕迹**。prettyJson try-catch：ToolResponse.responseData 是 JSON 字符串（M4 讲解稿第 9 题），但 terminate 这类工具的 responseData 可能不是合法 JSON（如 "[]" 或空），parse 失败回退原文展示，不崩组件。
- **7**：AntD X 的 Sender **非受控时**只在 onSubmit 回调拿值、自己不清空？不——真实根因：我们把发送按钮的点击走了 cua 坐标路径，Sender 内部对"点击发送"的清空依赖其默认行为，但 onSubmit 后我们没有把 value 置空，而 Sender 是非受控的（没传 value），所以文字残留。修复 = 转受控：value={inputValue} + onChange={setInputValue} + 发送时 setInputValue("")——受控组件的状态归我们管理，清空时机明确。教训：**带输入的组件一开始就受控**，别依赖库的隐式行为。
- **8**：现象：role/text 定位的 click/check 在 antd 组件上系统性超时（元素可见、无遮挡、elementFromPoint 命中正确），dom_cua 节点路径和坐标点击则全部成功。可能原因：Playwright 语义动作面（actionability 检查/事件合成）与 IAB 内嵌渲染层存在兼容问题。工程启示：①自动化依赖工具与被测栈的兼容性，验收受阻时按规范换路径（节点/坐标）而不是硬耗；②给团队的建议：核心链路回归优先考虑 **Playwright 官方 + 独立 Chromium**（标准协议栈）或组件级 Storybook/Vitest 测试，IAB 路径适合人工辅助式探索；③GUI 测试方法论本身（P0 计划、双证据、测修分离）比工具选择更重要——本轮就是按这套流程抓到输入框 bug 的。
