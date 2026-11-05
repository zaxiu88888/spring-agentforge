package com.agentforge.sse;

import com.agentforge.agent.AgentProgressListener;
import org.springframework.stereotype.Component;

/**
 * 按会话构造进度监听器：SSE 实现需要绑定 sessionId，
 * 不能用单例 Bean（多会话并发会串线，见 SseAgentProgressListener 注释）。
 * 测试可替换为返回 RecordingListener 的实现。
 */
@Component
public class ProgressListenerFactory {

    private final SseService sseService;

    public ProgressListenerFactory(SseService sseService) {
        this.sseService = sseService;
    }

    public AgentProgressListener create(String chatSessionId) {
        return new SseAgentProgressListener(sseService, chatSessionId);
    }
}
