package com.agentforge.model.response;

import com.agentforge.model.vo.AgentVO;
import com.agentforge.model.vo.ChatMessageVO;
import com.agentforge.model.vo.ChatSessionVO;
import com.agentforge.model.vo.DocumentVO;
import com.agentforge.model.vo.KnowledgeBaseVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 与 api-contract.md 各节响应结构逐字对齐 */
public final class Responses {

    private Responses() {
    }

    // ---- 创建类：返回 id ----
    @Data @Builder
    public static class CreateAgentResponse { private String agentId; }

    @Data @Builder
    public static class CreateChatSessionResponse { private String chatSessionId; }

    @Data @Builder
    public static class CreateChatMessageResponse { private String chatMessageId; }

    @Data @Builder
    public static class CreateKnowledgeBaseResponse { private String knowledgeBaseId; }

    @Data @Builder
    public static class CreateDocumentResponse { private String documentId; }

    // ---- 查询类：返回列表 ----
    @Data @Builder
    public static class GetAgentsResponse { private List<AgentVO> agents; }

    @Data @Builder
    public static class GetChatSessionsResponse { private List<ChatSessionVO> chatSessions; }

    @Data @Builder
    public static class GetChatMessagesResponse { private List<ChatMessageVO> chatMessages; }

    @Data @Builder
    public static class GetKnowledgeBasesResponse { private List<KnowledgeBaseVO> knowledgeBases; }

    @Data @Builder
    public static class GetDocumentsResponse { private List<DocumentVO> documents; }
}
