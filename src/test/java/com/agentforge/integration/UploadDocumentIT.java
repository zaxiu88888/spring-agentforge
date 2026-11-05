package com.agentforge.integration;

import com.agentforge.mapper.ChunkEmbeddingMapper;
import com.agentforge.mapper.DocumentMapper;
import com.agentforge.mapper.KnowledgeBaseMapper;
import com.agentforge.model.entity.KnowledgeBase;
import com.agentforge.service.DocumentFacadeService;
import com.agentforge.service.DocumentStorageService;
import com.agentforge.service.RagService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 上传全链路（mock 掉外部 embedding，DB/分块/落盘全真）：
 * 上传 → 文档记录 + filePath + 分块入库 + 磁盘文件；删除 → 全清。
 */
@Tag("integration")
@SpringBootTest
class UploadDocumentIT {

    @Autowired private DocumentFacadeService documentFacadeService;
    @Autowired private KnowledgeBaseMapper knowledgeBaseMapper;
    @Autowired private DocumentMapper documentMapper;
    @Autowired private ChunkEmbeddingMapper chunkEmbeddingMapper;
    @Autowired private DocumentStorageService storageService;

    @MockitoBean
    private RagService ragService;

    private KnowledgeBase insertKb() {
        KnowledgeBase kb = KnowledgeBase.builder().name("upload-it-" + System.nanoTime()).metadata("{}").build();
        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    @Test
    void uploadChunksAndDeleteRoundTrip() {
        KnowledgeBase kb = insertKb();
        String md = """
                这是使用说明前言。

                # 退货政策
                7 天无理由退货，运费自理。

                # 会员权益
                每月一张优惠券。
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "手册.md", "text/markdown", md.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 恒定向量（1024 维），隔离外部 embedding
        float[] vector = new float[1024];
        java.util.Arrays.fill(vector, 0.5f);
        when(ragService.embed(anyString())).thenReturn(vector);

        String docId = documentFacadeService.uploadDocument(kb.getId(), file);

        try {
            // 文档记录 + filePath
            var doc = documentMapper.selectById(docId);
            assertThat(doc).isNotNull();
            assertThat(doc.getMetadata()).contains("filePath");
            assertThat(doc.getSize()).isEqualTo(file.getSize());
            // 磁盘文件存在
            String filePath = extractFilePath(doc.getMetadata());
            assertThat(storageService.exists(filePath)).isTrue();
            // 分块：前言 + 2 个标题章节 = 3 块（查询向量必须同为 1024 维）
            String queryVector = com.agentforge.typehandler.PgVectorTypeHandler.toVectorLiteral(vector);
            assertThat(chunkEmbeddingMapper.similaritySearch(kb.getId(), queryVector, 10)).hasSize(3);

            // 删除：记录 + 分块（FK 级联） + 文件 全清
            documentFacadeService.deleteDocument(docId);
            assertThat(documentMapper.selectById(docId)).isNull();
            assertThat(chunkEmbeddingMapper.similaritySearch(kb.getId(), queryVector, 10)).isEmpty();
            assertThat(storageService.exists(filePath)).isFalse();
        } finally {
            knowledgeBaseMapper.deleteById(kb.getId());
        }
    }

    private static String extractFilePath(String metadataJson) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(metadataJson).path("filePath").asText();
        } catch (Exception e) {
            return "";
        }
    }

    @Test
    void uploadValidations() {
        KnowledgeBase kb = insertKb();
        try {
            assertThatThrownBy(() -> documentFacadeService.uploadDocument(kb.getId(), null))
                    .hasMessageContaining("文件不能为空");
            assertThatThrownBy(() -> documentFacadeService.uploadDocument(
                    kb.getId(), new MockMultipartFile("file", "a.pdf", null, new byte[]{1})))
                    .hasMessageContaining("仅支持 Markdown");
            assertThatThrownBy(() -> documentFacadeService.uploadDocument(
                    "00000000-0000-0000-0000-000000000000",
                    new MockMultipartFile("file", "a.md", null, new byte[]{1})))
                    .hasMessageContaining("知识库不存在");
        } finally {
            knowledgeBaseMapper.deleteById(kb.getId());
        }
    }

    @Test
    void embeddingFailureRollsBackEverything() {
        KnowledgeBase kb = insertKb();
        try {
            when(ragService.embed(anyString())).thenThrow(new RuntimeException("模拟向量化失败"));
            MockMultipartFile file = new MockMultipartFile(
                    "file", "失败.md", "text/markdown", "# T\n内容".getBytes());

            assertThatThrownBy(() -> documentFacadeService.uploadDocument(kb.getId(), file))
                    .hasMessageContaining("文档处理失败");

            // 回滚干净：无文档记录、无分块、无文件目录
            assertThat(documentMapper.selectByKbId(kb.getId())).isEmpty();
            float[] anyVector = new float[1024];
            assertThat(chunkEmbeddingMapper.similaritySearch(kb.getId(),
                    com.agentforge.typehandler.PgVectorTypeHandler.toVectorLiteral(anyVector), 10)).isEmpty();
        } finally {
            knowledgeBaseMapper.deleteById(kb.getId());
        }
    }
}
