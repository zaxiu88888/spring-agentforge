package com.agentforge.model.request;

import com.agentforge.model.dto.ChatMessageDTO;
import lombok.Data;

/** POST /api/chat-messages（api-contract.md §3）：M2 纯落库；M4 起落库后发布 ChatEvent 触发 Agent */
@Data
public class CreateChatMessageRequest {
    private String agentId;
    private String sessionId;
    private ChatMessageDTO.RoleType role;
    private String content;
}
