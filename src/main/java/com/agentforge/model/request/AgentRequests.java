package com.agentforge.model.request;

import com.agentforge.model.dto.AgentDTO;
import lombok.Data;

import java.util.List;

/** Agent 创建/更新请求（api-contract.md §1）；更新时全字段可空，null 不覆盖 */
public final class AgentRequests {

    private AgentRequests() {
    }

    @Data
    public static class CreateAgentRequest {
        private String name;
        private String description;
        private String systemPrompt;
        private String model;                        // glm-4.6 / deepseek-chat
        private List<String> allowedTools;
        private List<String> allowedKbs;
        private AgentDTO.ChatOptions chatOptions;
    }

    @Data
    public static class UpdateAgentRequest {
        private String name;
        private String description;
        private String systemPrompt;
        private String model;
        private List<String> allowedTools;
        private List<String> allowedKbs;
        private AgentDTO.ChatOptions chatOptions;
    }
}
