package com.agentforge.converter;

import com.agentforge.model.dto.ChatSessionDTO;
import com.agentforge.model.entity.ChatSession;
import com.agentforge.model.request.ChatSessionRequests.CreateChatSessionRequest;
import com.agentforge.model.request.ChatSessionRequests.UpdateChatSessionRequest;
import com.agentforge.model.vo.ChatSessionVO;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class ChatSessionConverter {

    public ChatSessionDTO toDTO(ChatSession entity) {
        Assert.notNull(entity, "entity 不能为空");
        return ChatSessionDTO.builder()
                .id(entity.getId())
                .agentId(entity.getAgentId())
                .title(entity.getTitle())
                .build();
    }

    public ChatSession toEntity(ChatSessionDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        return ChatSession.builder()
                .id(dto.getId())
                .agentId(dto.getAgentId())
                .title(dto.getTitle())
                .metadata("{}")
                .build();
    }

    public ChatSessionDTO toDTO(CreateChatSessionRequest request) {
        Assert.notNull(request, "request 不能为空");
        return ChatSessionDTO.builder()
                .agentId(request.getAgentId())
                .title(request.getTitle())
                .build();
    }

    public void updateDTOFromRequest(ChatSessionDTO dto, UpdateChatSessionRequest request) {
        Assert.notNull(dto, "dto 不能为空");
        Assert.notNull(request, "request 不能为空");
        if (request.getTitle() != null) dto.setTitle(request.getTitle());
    }

    public ChatSessionVO toVO(ChatSessionDTO dto, java.time.LocalDateTime updatedAt) {
        Assert.notNull(dto, "dto 不能为空");
        return ChatSessionVO.builder()
                .id(dto.getId())
                .agentId(dto.getAgentId())
                .title(dto.getTitle())
                .updatedAt(updatedAt)
                .build();
    }
}
