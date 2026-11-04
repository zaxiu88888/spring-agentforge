package com.agentforge.model.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDTO {

    private String id;
    private String name;
    private String description;
    private String systemPrompt;
    private ModelType model;
    private List<String> allowedTools;
    private List<String> allowedKbs;
    private ChatOptions chatOptions;

    /** 与 ChatClientRegistry 的 Bean 名严格对齐（tech-design.md §2 config） */
    public enum ModelType {
        GLM_4_6("glm-4.6"),
        DEEPSEEK_CHAT("deepseek-chat");

        private final String modelName;

        ModelType(String modelName) {
            this.modelName = modelName;
        }

        @JsonValue
        public String getModelName() {
            return modelName;
        }

        public static ModelType fromModelName(String name) {
            for (ModelType t : values()) {
                if (t.modelName.equals(name)) return t;
            }
            throw new IllegalArgumentException("未知模型: " + name);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatOptions {
        private Double temperature;
        private Double topP;
        private Integer messageLength;   // 记忆窗口条数

        public static ChatOptions defaultOptions() {
            return ChatOptions.builder()
                    .temperature(0.7)
                    .topP(1.0)
                    .messageLength(10)
                    .build();
        }
    }
}
