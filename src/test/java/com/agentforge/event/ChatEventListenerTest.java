package com.agentforge.event;

import com.agentforge.agent.AgentLoop;
import com.agentforge.agent.AgentRuntimeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 监听器语义：create+run 委托；装配异常被兜底吞掉（不向线程池外扩散） */
class ChatEventListenerTest {

    private final AgentRuntimeFactory factory = mock(AgentRuntimeFactory.class);
    private final ChatEventListener listener = new ChatEventListener(factory);

    @Test
    @DisplayName("事件触发工厂装配并运行循环")
    void delegatesToFactoryAndRuns() {
        AgentLoop loop = mock(AgentLoop.class);
        when(factory.create("a1", "s1")).thenReturn(loop);

        listener.onChatEvent(new ChatEvent("a1", "s1", "问题"));

        verify(factory).create("a1", "s1");
        verify(loop).run();
    }

    @Test
    @DisplayName("装配抛异常不向外传播（异步线程兜底）")
    void swallowsAssemblyFailure() {
        when(factory.create("a1", "s1")).thenThrow(new RuntimeException("Agent 不存在"));

        assertThatCode(() -> listener.onChatEvent(new ChatEvent("a1", "s1", "问题")))
                .doesNotThrowAnyException();
    }
}
