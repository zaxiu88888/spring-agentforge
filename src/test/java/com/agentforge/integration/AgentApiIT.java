package com.agentforge.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Agent CRUD 全链路（HTTP → Controller → Service → Mapper → agenthub_test）。
 * 覆盖 api-contract.md §1 四端点 + §0 ApiResponse 结构 + §9 错误语义。
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class AgentApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void agentCrudLifecycle() throws Exception {
        // 创建
        String body = """
                {
                  "name": "IT 数据助手",
                  "description": "集成测试",
                  "systemPrompt": "你是测试助手",
                  "model": "glm-4.6",
                  "allowedTools": ["sqlQuery", "getCurrentTime"],
                  "allowedKbs": [],
                  "chatOptions": {"temperature": 0.3, "topP": 0.9, "messageLength": 8}
                }
                """;
        MvcResult created = mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.agentId").isNotEmpty())
                .andReturn();
        String agentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("agentId").asText();

        // 列表包含，且 JSONB 字段往返无损
        MvcResult list = mockMvc.perform(get("/api/agents"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode agents = objectMapper.readTree(list.getResponse().getContentAsString())
                .path("data").path("agents");
        JsonNode mine = java.util.stream.StreamSupport.stream(agents.spliterator(), false)
                .filter(a -> agentId.equals(a.path("id").asText()))
                .findFirst().orElseThrow();
        assertThat(mine.path("model").asText()).isEqualTo("glm-4.6");
        assertThat(mine.path("allowedTools").get(0).asText()).isEqualTo("sqlQuery");
        assertThat(mine.path("chatOptions").path("messageLength").asInt()).isEqualTo(8);

        // PATCH：只改名字（null 不覆盖语义）
        mockMvc.perform(patch("/api/agents/{id}", agentId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"IT 改名\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        MvcResult afterPatch = mockMvc.perform(get("/api/agents")).andReturn();
        JsonNode patched = java.util.stream.StreamSupport.stream(
                        objectMapper.readTree(afterPatch.getResponse().getContentAsString())
                                .path("data").path("agents").spliterator(), false)
                .filter(a -> agentId.equals(a.path("id").asText()))
                .findFirst().orElseThrow();
        assertThat(patched.path("name").asText()).isEqualTo("IT 改名");
        assertThat(patched.path("systemPrompt").asText()).isEqualTo("你是测试助手"); // 未被覆盖

        // 删除
        mockMvc.perform(delete("/api/agents/{id}", agentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 删除不存在 → 业务错误（HTTP 200 + code 500 + 消息透传）
        mockMvc.perform(delete("/api/agents/{id}", agentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Agent 不存在: " + agentId));
    }

    @Test
    void createAgentValidation() throws Exception {
        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"model\": \"glm-4.6\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Agent 名称不能为空"));
    }
}
