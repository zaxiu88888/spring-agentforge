package com.agentforge.model.request;

import lombok.Data;

public final class KnowledgeBaseRequests {

    private KnowledgeBaseRequests() {
    }

    @Data
    public static class CreateKnowledgeBaseRequest {
        private String name;
        private String description;
    }

    @Data
    public static class UpdateKnowledgeBaseRequest {
        private String name;
        private String description;
    }
}
