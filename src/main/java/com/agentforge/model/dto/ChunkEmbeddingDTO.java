package com.agentforge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkEmbeddingDTO {
    private String id;
    private String kbId;
    private String docId;
    private String content;
    private MetaData metadata;
    private float[] embedding;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaData {
        private Integer chunkIndex;
        private String title;
    }
}
