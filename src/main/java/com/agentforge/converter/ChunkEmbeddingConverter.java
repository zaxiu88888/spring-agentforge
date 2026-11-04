package com.agentforge.converter;

import com.agentforge.model.dto.ChunkEmbeddingDTO;
import com.agentforge.model.entity.ChunkEmbedding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/** 仅供 RAG 内部使用，无对外 VO（api-contract.md 不暴露分块） */
@Component
@RequiredArgsConstructor
public class ChunkEmbeddingConverter {

    private final ObjectMapper objectMapper;

    public ChunkEmbeddingDTO toDTO(ChunkEmbedding entity) {
        Assert.notNull(entity, "entity 不能为空");
        return ChunkEmbeddingDTO.builder()
                .id(entity.getId())
                .kbId(entity.getKbId())
                .docId(entity.getDocId())
                .content(entity.getContent())
                .metadata(fromJson(entity.getMetadata()))
                .embedding(entity.getEmbedding())
                .build();
    }

    public ChunkEmbedding toEntity(ChunkEmbeddingDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        return ChunkEmbedding.builder()
                .id(dto.getId())
                .kbId(dto.getKbId())
                .docId(dto.getDocId())
                .content(dto.getContent())
                .metadata(toJson(dto.getMetadata()))
                .embedding(dto.getEmbedding())
                .build();
    }

    private ChunkEmbeddingDTO.MetaData fromJson(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return null;
        try {
            return objectMapper.readValue(json, ChunkEmbeddingDTO.MetaData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("分块元数据反序列化失败: " + json, e);
        }
    }

    private String toJson(ChunkEmbeddingDTO.MetaData metadata) {
        if (metadata == null) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("分块元数据序列化失败", e);
        }
    }
}
