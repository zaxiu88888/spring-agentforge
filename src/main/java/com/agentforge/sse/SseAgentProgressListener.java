package com.agentforge.sse;

import com.agentforge.model.vo.ChatMessageVO;
import lombok.extern.slf4j.Slf4j;

/**
 * AgentProgressListener 的 SSE 实现（tech-design.md §3.3 事件映射）：
 * onThinking → AI_THINKING；onExecuting → AI_EXECUTING；
 * onMessage → AI_GENERATED_CONTENT；onDone → AI_DONE。
 *
 * 🩹 非单例：按会话实例化（AgentRuntimeFactory.create 时 new，sessionId 绑定构造参数）。
 * 不能做成 @Component 单例——多会话并发时共享会话上下文会互相串线（M4 修过的同类缺陷）。
 *
 * 回调契约：绝不抛异常（AgentLoop 在 @Async 线程跑，回调失败不能影响循环）——
 * send 内部已吞掉无连接/连接失效，这里再兜一层防序列化等意外。
 */
@Slf4j
public class SseAgentProgressListener implements com.agentforge.agent.AgentProgressListener {

    private final SseService sseService;
    private final String chatSessionId;

    public SseAgentProgressListener(SseService sseService, String chatSessionId) {
        this.sseService = sseService;
        this.chatSessionId = chatSessionId;
    }

    @Override
    public void onThinking(String detail) {
        safelySend(SseMessage.thinking("正在思考: " + detail));
    }

    @Override
    public void onExecuting(String toolName) {
        safelySend(SseMessage.executing("正在执行: " + toolName));
    }

    @Override
    public void onMessage(ChatMessageVO message) {
        safelySend(SseMessage.generated(message));
    }

    @Override
    public void onDone(String errorSummary) {
        safelySend(SseMessage.done(errorSummary));
    }

    private void safelySend(SseMessage message) {
        try {
            sseService.send(chatSessionId, message);
        } catch (Exception e) {
            log.warn("SSE 推送回调异常（已吞掉，不影响 Agent 循环）session={}", chatSessionId, e);
        }
    }
}
