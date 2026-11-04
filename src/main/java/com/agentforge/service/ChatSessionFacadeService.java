package com.agentforge.service;

import com.agentforge.model.request.ChatSessionRequests.CreateChatSessionRequest;
import com.agentforge.model.request.ChatSessionRequests.UpdateChatSessionRequest;
import com.agentforge.model.vo.ChatSessionVO;

import java.util.List;

public interface ChatSessionFacadeService {

    String createChatSession(CreateChatSessionRequest request);

    List<ChatSessionVO> getChatSessions();

    List<ChatSessionVO> getChatSessionsByAgentId(String agentId);

    void updateChatSession(String chatSessionId, UpdateChatSessionRequest request);

    void deleteChatSession(String chatSessionId);
}
