package com.agentforge.converter;

import com.agentforge.model.dto.ChatMessageDTO;
import com.agentforge.model.entity.ChatMessage;
import com.agentforge.model.request.CreateChatMessageRequest;
import com.agentforge.model.vo.ChatMessageVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class ChatMessageConverter {

    private final ObjectMapper objectMapper;

    public ChatMessageDTO toDTO(ChatMessage entity) {
        Assert.notNull(entity, "entity 不能为空");
        return ChatMessageDTO.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .role(ChatMessageDTO.RoleType.fromValue(entity.getRole()))
                .content(entity.getContent())
                .metadata(fromJson(entity.getMetadata()))
                .build();
    }

    public ChatMessage toEntity(ChatMessageDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        return ChatMessage.builder()
                .id(dto.getId())
                .sessionId(dto.getSessionId())
                .role(dto.getRole() == null ? null : dto.getRole().getValue())
                .content(dto.getContent())
                .metadata(toJson(dto.getMetadata()))
                .build();
    }

    public ChatMessageDTO toDTO(CreateChatMessageRequest request) {
        Assert.notNull(request, "request 不能为空");
        return ChatMessageDTO.builder()
                .sessionId(request.getSessionId())
                .role(request.getRole())
                .content(request.getContent())
                .build();
    }

    public ChatMessageVO toVO(ChatMessageDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        return ChatMessageVO.builder()
                .id(dto.getId())
                .sessionId(dto.getSessionId())
                .role(dto.getRole() == null ? null : dto.getRole().getValue())
                .content(dto.getContent())
                .metadata(dto.getMetadata() == null ? null : ChatMessageVO.MetaData.builder()
                        .toolCalls(dto.getMetadata().getToolCalls())
                        .toolResponse(dto.getMetadata().getToolResponse())
                        .build())
                .build();
    }

    private ChatMessageDTO.MetaData fromJson(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return null;
        try {
            return objectMapper.readValue(json, ChatMessageDTO.MetaData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("消息元数据反序列化失败: " + json, e);
        }
    }

    private String toJson(ChatMessageDTO.MetaData metadata) {
        if (metadata == null) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("消息元数据序列化失败", e);
        }
    }
}
