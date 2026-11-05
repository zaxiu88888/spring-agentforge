package com.agentforge.service;

import com.agentforge.model.dto.ChatMessageDTO;
import com.agentforge.model.request.CreateChatMessageRequest;
import com.agentforge.model.vo.ChatMessageVO;

import java.util.List;

public interface ChatMessageFacadeService {

    /** 外部入口（HTTP）：落库。M4 起将在此发布 ChatEvent 触发 Agent 异步执行 */
    String createChatMessage(CreateChatMessageRequest request);

    /** Agent 内部产出的消息：落库但不发布事件（防事件风暴，tech-design.md §3.1） */
    String agentCreateChatMessage(ChatMessageDTO dto);

    List<ChatMessageVO> getChatMessagesBySessionId(String sessionId);

    /** 最近 N 条（时间正序），AgentRuntimeFactory 回放记忆用 */
    List<ChatMessageDTO> getRecentMessages(String sessionId, int limit);
}
