package com.agentforge.event;

import com.agentforge.agent.AgentRuntimeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 事件驱动入口：HTTP 线程只负责落库与返回，Agent 的 20 步循环在 agentExecutor 线程池异步执行。
 * AgentLoop 自身吞掉循环内异常；此处兜底捕获装配阶段异常（Agent 不存在/模型未注册等）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private final AgentRuntimeFactory agentRuntimeFactory;

    @Async("agentExecutor")
    @EventListener
    public void onChatEvent(ChatEvent event) {
        log.info("收到 ChatEvent agent={} session={}", event.agentId(), event.sessionId());
        try {
            agentRuntimeFactory.create(event.agentId(), event.sessionId()).run();
        } catch (Exception e) {
            log.error("Agent 执行失败 agent={} session={}", event.agentId(), event.sessionId(), e);
        }
    }
}
