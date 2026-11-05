package com.agentforge.agent.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentTimeToolTest {

    private final CurrentTimeTool tool = new CurrentTimeTool();

    @Test
    @DisplayName("默认格式 yyyy-MM-dd HH:mm:ss")
    void defaultFormat() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(CurrentTimeTool.DEFAULT_FORMAT));
        assertThat(tool.getCurrentTime(null)).isEqualTo(now);
        assertThat(tool.getCurrentTime("  ")).isEqualTo(now);
    }

    @Test
    @DisplayName("自定义格式生效")
    void customFormat() {
        String result = tool.getCurrentTime("yyyy/MM/dd");
        assertThat(result).matches("\\d{4}/\\d{2}/\\d{2}");
    }

    @Test
    @DisplayName("非法格式回退默认而非抛错（不打断 Agent）")
    void invalidFormatFallsBack() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(CurrentTimeTool.DEFAULT_FORMAT));
        assertThat(tool.getCurrentTime("not-a-pattern@@")).isEqualTo(now);
    }
}
