package com.agentforge.service.impl;

import com.agentforge.converter.ChatMessageConverter;
import com.agentforge.exception.BizException;
import com.agentforge.mapper.ChatMessageMapper;
import com.agentforge.mapper.ChatSessionMapper;
import com.agentforge.model.dto.ChatMessageDTO;
import com.agentforge.model.entity.ChatMessage;
import com.agentforge.model.request.CreateChatMessageRequest;
import com.agentforge.model.vo.ChatMessageVO;
import com.agentforge.service.ChatMessageFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageFacadeServiceImpl implements ChatMessageFacadeService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageConverter chatMessageConverter;

    @Override
    public String createChatMessage(CreateChatMessageRequest request) {
        validate(request);
        return doCreate(chatMessageConverter.toDTO(request));
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
