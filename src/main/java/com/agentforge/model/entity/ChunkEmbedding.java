package com.agentforge.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 文档分块 + 向量（embedding-3，1024 维）；embedding 由 PgVectorTypeHandler 映射 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkEmbedding {
    private String id;
    private String kbId;
    private String docId;
    private String content;
    private String metadata;       // JSON：chunk 序号、章节标题等
    private float[] embedding;     // VECTOR(1024)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
