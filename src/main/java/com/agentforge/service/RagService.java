package com.agentforge.service;

import com.agentforge.model.dto.ChunkEmbeddingDTO;

import java.util.List;

public interface RagService {

    float[] embed(String text);

    /** 向量相似度检索：embed(query) → pgvector L2 top-k（tech-design.md §3.5） */
    List<ChunkEmbeddingDTO> similaritySearch(String kbId, String query, int topK);
}
