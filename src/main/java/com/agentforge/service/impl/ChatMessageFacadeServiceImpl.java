package com.agentforge.service.impl;

import com.agentforge.converter.ChatMessageConverter;
import com.agentforge.event.ChatEvent;
import com.agentforge.exception.BizException;
import com.agentforge.mapper.ChatMessageMapper;
import com.agentforge.mapper.ChatSessionMapper;
import com.agentforge.model.dto.ChatMessageDTO;
import com.agentforge.model.entity.ChatMessage;
import com.agentforge.model.request.CreateChatMessageRequest;
import com.agentforge.model.vo.ChatMessageVO;
import com.agentforge.service.ChatMessageFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageFacadeServiceImpl implements ChatMessageFacadeService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageConverter chatMessageConverter;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String createChatMessage(CreateChatMessageRequest request) {
        validate(request);
        String id = doCreate(chatMessageConverter.toDTO(request));
        // 只有用户消息才触发 Agent（assistant/tool 由 Agent 自己产生，不发事件防递归）
        if (request.getRole() == ChatMessageDTO.RoleType.USER) {
            eventPublisher.publishEvent(new ChatEvent(
                    request.getAgentId(), request.getSessionId(), request.getContent()));
        }
        return id;
    }

    @Override
    public String agentCreateChatMessage(ChatMessageDTO dto) {
        return doCreate(dto);
    }

    @Override
    public List<ChatMessageVO> getChatMessagesBySessionId(String sessionId) {
        if (chatSessionMapper.selectById(sessionId) == null) {
            throw new BizException("会话不存在: " + sessionId);
        }
        return chatMessageMapper.selectBySessionId(sessionId).stream()
                .map(chatMessageConverter::toDTO)
                .map(chatMessageConverter::toVO)
                .toList();
    }

    @Override
    public List<ChatMessageDTO> getRecentMessages(String sessionId, int limit) {
        List<ChatMessage> recent = chatMessageMapper.selectRecent(sessionId, limit);
        // selectRecent 为 DESC（最近在前），反转为时间正序
        List<ChatMessage> asc = new ArrayList<>(recent);
        java.util.Collections.reverse(asc);
        return asc.stream().map(chatMessageConverter::toDTO).toList();
    }

    private void validate(CreateChatMessageRequest request) {
        if (!StringUtils.hasText(request.getSessionId())) {
            throw new BizException("sessionId 不能为空");
        }
        if (request.getRole() == null) {
            throw new BizException("role 不能为空");
        }
        if (chatSessionMapper.selectById(request.getSessionId()) == null) {
            throw new BizException("会话不存在: " + request.getSessionId());
        }
    }

    private String doCreate(ChatMessageDTO dto) {
        ChatMessage entity = chatMessageConverter.toEntity(dto);
        chatMessageMapper.insert(entity);
        return entity.getId();
    }
}
