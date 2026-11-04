package com.agentforge.model.request;

import lombok.Data;

public final class ChatSessionRequests {

    private ChatSessionRequests() {
    }

    @Data
    public static class CreateChatSessionRequest {
        private String agentId;
        private String title;
    }

    @Data
    public static class UpdateChatSessionRequest {
        private String title;
    }
}
