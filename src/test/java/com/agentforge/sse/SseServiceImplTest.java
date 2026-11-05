package com.agentforge.sse;

import com.agentforge.model.vo.ChatMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** SseService 连接池语义（🩹核心：未连接不抛异常；重复连接替换；断连清理） */
class SseServiceImplTest {

    private final SseServiceImpl sseService = new SseServiceImpl(new ObjectMapper());

    @Test
    @DisplayName("🩹 无连接时 send 返回 false 且不抛异常（原型缺陷修复的核心断言）")
    void sendWithoutConnectionReturnsFalse() {
        assertThatCode(() -> sseService.send("no-such-session", SseMessage.thinking("思考中")))
                .doesNotThrowAnyException();
        assertThat(sseService.send("no-such-session", SseMessage.thinking("思考中"))).isFalse();
    }

    @Test
    @DisplayName("有连接时 send 返回 true；disconnect 后再发返回 false")
    void sendWithConnection() {
        SseEmitter emitter = sseService.connect("s1");
        assertThat(emitter).isNotNull();
        assertThat(sseService.send("s1", SseMessage.executing("正在执行: sqlQuery"))).isTrue();

        sseService.disconnect("s1");
        assertThat(sseService.send("s1", SseMessage.thinking("思考中"))).isFalse();
    }

    @Test
    @DisplayName("同会话重复连接：新连接生效，旧连接被完成（避免双连接双份推送）")
    void reconnectReplacesOldEmitter() {
        SseEmitter oldEmitter = sseService.connect("s2");
        SseEmitter newEmitter = sseService.connect("s2");
        assertThat(newEmitter).isNotSameAs(oldEmitter);
        assertThat(sseService.send("s2", SseMessage.done(null))).isTrue();
    }

    @Test
    @DisplayName("四类事件工厂：type/payload 字段正确")
    void eventFactories() {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId("m1");
        vo.setRole("assistant");
        vo.setContent("答案");

        assertThat(SseMessage.thinking("正在思考: x").type()).isEqualTo(SseMessage.Type.AI_THINKING);
        assertThat(SseMessage.executing("正在执行: y").payload().statusText()).contains("y");
        assertThat(SseMessage.generated(vo).type()).isEqualTo(SseMessage.Type.AI_GENERATED_CONTENT);
        assertThat(SseMessage.generated(vo).metadata().chatMessageId()).isEqualTo("m1");
        assertThat(SseMessage.done("超时").payload().done()).isTrue();
        assertThat(SseMessage.done(null).type()).isEqualTo(SseMessage.Type.AI_DONE);
    }
}
