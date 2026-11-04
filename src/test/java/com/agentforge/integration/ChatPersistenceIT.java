package com.agentforge.integration;

import com.agentforge.mapper.AgentMapper;
import com.agentforge.mapper.ChatMessageMapper;
import com.agentforge.mapper.ChatSessionMapper;
import com.agentforge.model.dto.ChatMessageDTO;
import com.agentforge.model.entity.Agent;
import com.agentforge.model.entity.ChatMessage;
import com.agentforge.model.entity.ChatSession;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话/消息持久化核心行为：FK 级联、时间排序、工具元数据 JSONB 往返（tech-design.md §3.2 记忆协议的底层）。
 */
@Tag("integration")
@SpringBootTest
class ChatPersistenceIT {

    @Autowired private AgentMapper agentMapper;
    @Autowired private ChatSessionMapper chatSessionMapper;
    @Autowired private ChatMessageMapper chatMessageMapper;

    @Test
    void sessionCascadeDeletesMessages() {
        String sessionId = insertSession("cascade-it");
        insertMessage(sessionId, "user", "第一条", null);
        insertMessage(sessionId, "user", "第二条", null);
        assertThat(chatMessageMapper.selectBySessionId(sessionId)).hasSize(2);

        chatSessionMapper.deleteById(sessionId);
        assertThat(chatSessionMapper.selectById(sessionId)).isNull();
        assertThat(chatMessageMapper.selectBySessionId(sessionId)).isEmpty();   // FK CASCADE 生效
    }

    @Test
    void messagesOrderedByCreatedAtAndRecentLimit() {
        String sessionId = insertSession("order-it");
        try {
            for (int i = 1; i <= 5; i++) {
                insertMessage(sessionId, "user", "msg-" + i, null);
                sleepMillis(15);   // 保证 created_at 可分辨
            }
            List<ChatMessage> asc = chatMessageMapper.selectBySessionId(sessionId);
            assertThat(asc).hasSize(5);
            assertThat(asc.get(0).getContent()).isEqualTo("msg-1");
            assertThat(asc.get(4).getContent()).isEqualTo("msg-5");

            List<ChatMessage> recent = chatMessageMapper.selectRecent(sessionId, 3);
            assertThat(recent).hasSize(3);
            assertThat(recent.get(0).getContent()).isEqualTo("msg-5");   // DESC：最近在前
        } finally {
            chatSessionMapper.deleteById(sessionId);
        }
    }

    @Test
    void toolCallMetadataRoundTrip() throws Exception {
        String sessionId = insertSession("metadata-it");
        try {
            // assistant + toolCalls
            AssistantMessage.ToolCall call =
                    new AssistantMessage.ToolCall("call-1", "function", "sqlQuery", "{\"sql\":\"SELECT 1\"}");
            ChatMessageDTO.MetaData assistantMeta = ChatMessageDTO.MetaData.builder()
                    .toolCalls(List.of(call)).build();
            String assistantId = insertMessage(sessionId, "assistant", "", assistantMeta);

            // tool + toolResponse
            ChatMessageDTO.MetaData toolMeta = ChatMessageDTO.MetaData.builder()
                    .toolResponse(new ToolResponseMessage.ToolResponse("call-1", "sqlQuery", "count=10"))
                    .build();
            insertMessage(sessionId, "tool", "count=10", toolMeta);

            List<ChatMessage> stored = chatMessageMapper.selectBySessionId(sessionId);
            assertThat(stored).hasSize(2);

            // 解析 JSON 断言（避免依赖 Jackson 序列化的空格格式）
            com.fasterxml.jackson.databind.JsonNode storedAssistantMeta =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(stored.get(0).getMetadata());
            com.fasterxml.jackson.databind.JsonNode storedToolMeta =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(stored.get(1).getMetadata());
            assertThat(storedAssistantMeta.path("toolCalls").get(0).path("name").asText()).isEqualTo("sqlQuery");
            assertThat(storedAssistantMeta.path("toolCalls").get(0).path("arguments").asText()).contains("SELECT 1");
            assertThat(storedToolMeta.path("toolResponse").path("responseData").asText()).isEqualTo("count=10");
            assertThat(assistantId).isNotNull();
        } finally {
            chatSessionMapper.deleteById(sessionId);
        }
    }

    // ---- helpers ----

    private String insertSession(String title) {
        Agent agent = Agent.builder().name("it-agent-" + UUID.randomUUID()).model("glm-4.6").build();
        agentMapper.insert(agent);
        ChatSession session = ChatSession.builder()
                .agentId(agent.getId()).title(title).metadata("{}").build();
        chatSessionMapper.insert(session);
        return session.getId();
    }

    private String insertMessage(String sessionId, String role, String content, ChatMessageDTO.MetaData metadata) {
        String metaJson = null;
        if (metadata != null) {
            try {
                metaJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        ChatMessage msg = ChatMessage.builder()
                .sessionId(sessionId).role(role).content(content).metadata(metaJson).build();
        chatMessageMapper.insert(msg);
        return msg.getId();
    }

    private void sleepMillis(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
