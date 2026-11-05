package com.agentforge.sse;

import com.agentforge.model.vo.ChatMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 监听器 → SSE 事件映射（4 事件 + 回调吞异常契约） */
class SseAgentProgressListenerTest {

    private final SseService sseService = mock(SseService.class);
    private final SseAgentProgressListener listener = new SseAgentProgressListener(sseService, "sess-1");

    @Test
    @DisplayName("四个回调映射到四类事件，且都发给绑定的会话")
    void mapsCallbacksToEvents() {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId("m1");

        listener.onThinking("决定调用: sqlQuery");
        listener.onExecuting("sqlQuery");
        listener.onMessage(vo);
        listener.onDone(null);

        ArgumentCaptor<SseMessage> captor = ArgumentCaptor.forClass(SseMessage.class);
        verify(sseService, times(4)).send(eq("sess-1"), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(SseMessage::type)
                .containsExactly(
                        SseMessage.Type.AI_THINKING,
                        SseMessage.Type.AI_EXECUTING,
                        SseMessage.Type.AI_GENERATED_CONTENT,
                        SseMessage.Type.AI_DONE);
        // 状态文本组装
        verify(sseService).send(eq("sess-1"), Mockito.argThat(
                (SseMessage m) -> m.type() == SseMessage.Type.AI_THINKING
                        && m.payload().statusText().contains("sqlQuery")));
    }

    @Test
    @DisplayName("🩹 回调契约：sseService 抛异常也不向上传播（不影响 Agent 循环）")
    void callbacksNeverThrow() {
        when(sseService.send(any(), any())).thenThrow(new RuntimeException("连接已死"));
        assertThatCode(() -> {
            listener.onThinking("x");
            listener.onExecuting("y");
            listener.onMessage(new ChatMessageVO());
            listener.onDone(null);
        }).doesNotThrowAnyException();
    }
}
