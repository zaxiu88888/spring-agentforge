package com.agentforge.converter;

import com.agentforge.model.dto.KnowledgeBaseDTO;
import com.agentforge.model.entity.KnowledgeBase;
import com.agentforge.model.request.KnowledgeBaseRequests.CreateKnowledgeBaseRequest;
import com.agentforge.model.request.KnowledgeBaseRequests.UpdateKnowledgeBaseRequest;
import com.agentforge.model.vo.KnowledgeBaseVO;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class KnowledgeBaseConverter {

    public KnowledgeBaseDTO toDTO(KnowledgeBase entity) {
        Assert.notNull(entity, "entity 不能为空");
        return KnowledgeBaseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }

    public KnowledgeBase toEntity(KnowledgeBaseDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        return KnowledgeBase.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .metadata("{}")
                .build();
    }

    public KnowledgeBaseDTO toDTO(CreateKnowledgeBaseRequest request) {
        Assert.notNull(request, "request 不能为空");
        return KnowledgeBaseDTO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public void updateDTOFromRequest(KnowledgeBaseDTO dto, UpdateKnowledgeBaseRequest request) {
        Assert.notNull(dto, "dto 不能为空");
        Assert.notNull(request, "request 不能为空");
        if (request.getName() != null) dto.setName(request.getName());
        if (request.getDescription() != null) dto.setDescription(request.getDescription());
    }

    public KnowledgeBaseVO toVO(KnowledgeBaseDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        return KnowledgeBaseVO.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
    }
}
