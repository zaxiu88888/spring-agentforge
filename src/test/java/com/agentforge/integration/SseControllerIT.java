package com.agentforge.integration;

import com.agentforge.sse.SseService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /sse/connect/{sessionId} 契约：text/event-stream + 异步启动 + init 事件（api-contract.md §8） */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class SseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SseService sseService;

    @Test
    void connectReturnsEventStreamWithInit() throws Exception {
        String sessionId = "sse-it-" + System.nanoTime();
        MvcResult result = mockMvc.perform(get("/sse/connect/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())   // SSE 是异步响应
                .andReturn();

        // init 事件已写入响应（MockMvc 下 emitter 立即刷 init）
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("connected");

        // 连接已入池：向该会话推送返回 true
        assertThat(sseService.send(sessionId,
                com.agentforge.sse.SseMessage.thinking("测试"))).isTrue();

        sseService.disconnect(sessionId);
    }

    @Test
    void ssePathHasNoApiPrefix() throws Exception {
        // /api/sse/... 不存在（真 404），SSE 端点在 /sse 下
        mockMvc.perform(get("/api/sse/connect/x"))
                .andExpect(status().isNotFound());
    }
}
