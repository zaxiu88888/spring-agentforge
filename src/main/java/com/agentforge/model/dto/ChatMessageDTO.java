package com.agentforge.model.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private String id;
    private String sessionId;
    private RoleType role;
    private String content;
    private MetaData metadata;

    public enum RoleType {
        USER("user"),
        ASSISTANT("assistant"),
        SYSTEM("system"),
        TOOL("tool");

        private final String value;

        RoleType(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        public static RoleType fromValue(String value) {
            for (RoleType r : values()) {
                if (r.value.equals(value)) return r;
            }
            throw new IllegalArgumentException("未知角色: " + value);
        }
    }

    /**
     * 工具调用持久化协议（tech-design.md §3.2）：
     * assistant 消息带 toolCalls，tool 消息带 toolResponse，直接复用 Spring AI 的 record 类型做 JSON 序列化。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaData {
        private List<AssistantMessage.ToolCall> toolCalls;
        private ToolResponseMessage.ToolResponse toolResponse;
    }
}
