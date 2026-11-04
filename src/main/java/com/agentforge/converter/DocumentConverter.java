package com.agentforge.converter;

import com.agentforge.model.dto.DocumentDTO;
import com.agentforge.model.entity.Document;
import com.agentforge.model.vo.DocumentVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class DocumentConverter {

    private final ObjectMapper objectMapper;

    public DocumentDTO toDTO(Document entity) {
        Assert.notNull(entity, "entity 不能为空");
        return DocumentDTO.builder()
                .id(entity.getId())
                .kbId(entity.getKbId())
                .filename(entity.getFilename())
                .filetype(entity.getFiletype())
                .size(entity.getSize())
                .metadata(fromJson(entity.getMetadata()))
                .build();
    }

    public Document toEntity(DocumentDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        return Document.builder()
                .id(dto.getId())
                .kbId(dto.getKbId())
                .filename(dto.getFilename())
                .filetype(dto.getFiletype())
                .size(dto.getSize())
                .metadata(toJson(dto.getMetadata()))
                .build();
    }

    public DocumentVO toVO(DocumentDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        return DocumentVO.builder()
                .id(dto.getId())
                .kbId(dto.getKbId())
                .filename(dto.getFilename())
                .filetype(dto.getFiletype())
                .size(dto.getSize())
                .build();
    }

    private DocumentDTO.MetaData fromJson(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return null;
        try {
            return objectMapper.readValue(json, DocumentDTO.MetaData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("文档元数据反序列化失败: " + json, e);
        }
    }

    private String toJson(DocumentDTO.MetaData metadata) {
        if (metadata == null) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("文档元数据序列化失败", e);
        }
    }
}
