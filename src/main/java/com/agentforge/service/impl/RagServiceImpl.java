package com.agentforge.service.impl;

import com.agentforge.mapper.ChunkEmbeddingMapper;
import com.agentforge.model.dto.ChunkEmbeddingDTO;
import com.agentforge.model.entity.ChunkEmbedding;
import com.agentforge.rag.ZhipuEmbeddingClient;
import com.agentforge.service.RagService;
import com.agentforge.typehandler.PgVectorTypeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final ZhipuEmbeddingClient embeddingClient;
    private final ChunkEmbeddingMapper chunkEmbeddingMapper;
    private final com.agentforge.converter.ChunkEmbeddingConverter chunkEmbeddingConverter;

    @Override
    public float[] embed(String text) {
        return embeddingClient.embed(text);
    }

    @Override
    public List<ChunkEmbeddingDTO> similaritySearch(String kbId, String query, int topK) {
        String queryVector = PgVectorTypeHandler.toVectorLiteral(embeddingClient.embed(query));
        return chunkEmbeddingMapper.similaritySearch(kbId, queryVector, topK).stream()
                .map(chunkEmbeddingConverter::toDTO)
                .toList();
    }
}
