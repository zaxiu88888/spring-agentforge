package com.agentforge.service.impl;

import com.agentforge.converter.ChatSessionConverter;
import com.agentforge.exception.BizException;
import com.agentforge.mapper.AgentMapper;
import com.agentforge.mapper.ChatSessionMapper;
import com.agentforge.model.dto.ChatSessionDTO;
import com.agentforge.model.entity.ChatSession;
import com.agentforge.model.request.ChatSessionRequests.CreateChatSessionRequest;
import com.agentforge.model.request.ChatSessionRequests.UpdateChatSessionRequest;
import com.agentforge.model.vo.ChatSessionVO;
import com.agentforge.service.ChatSessionFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSessionFacadeServiceImpl implements ChatSessionFacadeService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatSessionConverter chatSessionConverter;
    private final AgentMapper agentMapper;

    @Override
    public String createChatSession(CreateChatSessionRequest request) {
        if (StringUtils.hasText(request.getAgentId()) && agentMapper.selectById(request.getAgentId()) == null) {
            throw new BizException("Agent 不存在: " + request.getAgentId());
        }
        ChatSessionDTO dto = chatSessionConverter.toDTO(request);
        ChatSession entity = chatSessionConverter.toEntity(dto);
        chatSessionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public List<ChatSessionVO> getChatSessions() {
        return chatSessionMapper.selectAll().stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public List<ChatSessionVO> getChatSessionsByAgentId(String agentId) {
        return chatSessionMapper.selectByAgentId(agentId).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public void updateChatSession(String chatSessionId, UpdateChatSessionRequest request) {
        ChatSession existing = requireSession(chatSessionId);
        ChatSessionDTO dto = chatSessionConverter.toDTO(existing);
        chatSessionConverter.updateDTOFromRequest(dto, request);
        ChatSession entity = chatSessionConverter.toEntity(dto);
        entity.setId(chatSessionId);
        chatSessionMapper.updateById(entity);
    }

    @Override
    public void deleteChatSession(String chatSessionId) {
        requireSession(chatSessionId);
        chatSessionMapper.deleteById(chatSessionId);   // chat_message 经 FK ON DELETE CASCADE 连带删除
    }

    private ChatSession requireSession(String chatSessionId) {
        ChatSession session = chatSessionMapper.selectById(chatSessionId);
        if (session == null) {
            throw new BizException("会话不存在: " + chatSessionId);
        }
        return session;
    }

    private ChatSessionVO toVO(ChatSession entity) {
        return chatSessionConverter.toVO(chatSessionConverter.toDTO(entity), entity.getUpdatedAt());
    }
}
