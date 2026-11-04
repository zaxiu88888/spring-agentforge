package com.agentforge.integration;

import com.agentforge.mapper.ChunkEmbeddingMapper;
import com.agentforge.mapper.DocumentMapper;
import com.agentforge.mapper.KnowledgeBaseMapper;
import com.agentforge.model.entity.ChunkEmbedding;
import com.agentforge.model.entity.Document;
import com.agentforge.model.entity.KnowledgeBase;
import com.agentforge.typehandler.PgVectorTypeHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 向量入库 + pgvector L2 相似度检索（RAG 底层，tech-design.md §3.5） */
@Tag("integration")
@SpringBootTest
class VectorSearchIT {

    @Autowired private KnowledgeBaseMapper knowledgeBaseMapper;
    @Autowired private DocumentMapper documentMapper;
    @Autowired private ChunkEmbeddingMapper chunkEmbeddingMapper;

    @Test
    void similaritySearchReturnsNearestFirst() {
        // 准备 KB + 文档
        KnowledgeBase kb = KnowledgeBase.builder().name("vec-it-" + UUID.randomUUID()).metadata("{}").build();
        knowledgeBaseMapper.insert(kb);
        Document doc = Document.builder().kbId(kb.getId()).filename("it.md").filetype("md").metadata("{}").build();
        documentMapper.insert(doc);
        try {
            // 三个 1024 维向量：A 与查询向量接近，B 较远
            float[] query = fill(0.1f);
            float[] near = fill(0.1001f);
            float[] far = fill(0.9f);

            insertChunk(kb.getId(), doc.getId(), "near-chunk", near);
            insertChunk(kb.getId(), doc.getId(), "far-chunk", far);

            List<ChunkEmbedding> hits = chunkEmbeddingMapper.similaritySearch(
                    kb.getId(), PgVectorTypeHandler.toVectorLiteral(query), 2);

            assertThat(hits).hasSize(2);
            assertThat(hits.get(0).getContent()).isEqualTo("near-chunk");   // L2 升序，最近在前
            assertThat(hits.get(1).getContent()).isEqualTo("far-chunk");

            // 向量读回无损
            assertThat(hits.get(0).getEmbedding()).hasSize(1024);
            assertThat(hits.get(0).getEmbedding()[0]).isCloseTo(0.1001f, within(1e-6f));
        } finally {
            knowledgeBaseMapper.deleteById(kb.getId());   // 级联清 document + chunk
        }
    }

    @Test
    void searchIsolatedByKbId() {
        KnowledgeBase kbA = KnowledgeBase.builder().name("kbA-" + UUID.randomUUID()).metadata("{}").build();
        KnowledgeBase kbB = KnowledgeBase.builder().name("kbB-" + UUID.randomUUID()).metadata("{}").build();
        knowledgeBaseMapper.insert(kbA);
        knowledgeBaseMapper.insert(kbB);
        Document docB = Document.builder().kbId(kbB.getId()).filename("b.md").filetype("md").metadata("{}").build();
        documentMapper.insert(docB);
        try {
            insertChunk(kbB.getId(), docB.getId(), "only-in-B", fill(0.1f));
            // 用 A 的 kbId 查：查不到 B 的 chunk（知识库隔离）
            List<ChunkEmbedding> hits = chunkEmbeddingMapper.similaritySearch(
                    kbA.getId(), PgVectorTypeHandler.toVectorLiteral(fill(0.1f)), 3);
            assertThat(hits).isEmpty();
        } finally {
            knowledgeBaseMapper.deleteById(kbA.getId());
            knowledgeBaseMapper.deleteById(kbB.getId());
        }
    }

    // ---- helpers ----

    private void insertChunk(String kbId, String docId, String content, float[] embedding) {
        chunkEmbeddingMapper.insert(ChunkEmbedding.builder()
                .kbId(kbId).docId(docId).content(content).metadata("{}").embedding(embedding).build());
    }

    private static float[] fill(float v) {
        float[] a = new float[1024];
        java.util.Arrays.fill(a, v);
        return a;
    }

    private static org.assertj.core.data.Offset<Float> within(float f) {
        return org.assertj.core.data.Offset.offset(f);
    }
}
