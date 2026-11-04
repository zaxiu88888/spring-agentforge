package com.agentforge.integration;

import com.agentforge.mapper.DocumentMapper;
import com.agentforge.mapper.KnowledgeBaseMapper;
import com.agentforge.model.entity.Document;
import com.agentforge.model.entity.KnowledgeBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 知识库批量查询 + 文档按 KB 归属 + 删除级联（M2 Agent 装配要用 selectByIdBatch） */
@Tag("integration")
@SpringBootTest
class KbDocumentIT {

    @Autowired private KnowledgeBaseMapper knowledgeBaseMapper;
    @Autowired private DocumentMapper documentMapper;

    @Test
    void batchSelectAndDocumentCascade() {
        KnowledgeBase kb1 = insertKb("batch-1");
        KnowledgeBase kb2 = insertKb("batch-2");
        Document doc1 = insertDoc(kb1.getId(), "a.md");
        insertDoc(kb1.getId(), "b.md");

        try {
            // 批量查询（AgentRuntimeFactory 解析 allowed_kbs 用）
            List<KnowledgeBase> batch = knowledgeBaseMapper.selectByIdBatch(
                    List.of(kb1.getId(), kb2.getId(), UUID.randomUUID().toString()));
            assertThat(batch).hasSize(2);

            // 文档按 kbId 归属
            List<Document> docs = documentMapper.selectByKbId(kb1.getId());
            assertThat(docs).hasSize(2);

            // 删除单个文档
            documentMapper.deleteById(doc1.getId());
            assertThat(documentMapper.selectByKbId(kb1.getId())).hasSize(1);

            // 删除 KB 级联清空文档
            knowledgeBaseMapper.deleteById(kb1.getId());
            assertThat(documentMapper.selectByKbId(kb1.getId())).isEmpty();
        } finally {
            knowledgeBaseMapper.deleteById(kb1.getId());
            knowledgeBaseMapper.deleteById(kb2.getId());
        }
    }

    private KnowledgeBase insertKb(String name) {
        KnowledgeBase kb = KnowledgeBase.builder().name(name + "-" + UUID.randomUUID()).metadata("{}").build();
        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    private Document insertDoc(String kbId, String filename) {
        Document doc = Document.builder().kbId(kbId).filename(filename).filetype("md")
                .size(1024L).metadata("{}").build();
        documentMapper.insert(doc);
        return doc;
    }
}
