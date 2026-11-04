package com.agentforge.mapper;

import com.agentforge.model.entity.ChunkEmbedding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChunkEmbeddingMapper {
    int insert(ChunkEmbedding chunk);

    /** pgvector L2 相似度检索：ORDER BY embedding <-> queryVector（top-k，tech-design.md §3.5） */
    List<ChunkEmbedding> similaritySearch(@Param("kbId") String kbId,
                                          @Param("queryVector") String queryVectorLiteral,
                                          @Param("limit") int limit);
}
