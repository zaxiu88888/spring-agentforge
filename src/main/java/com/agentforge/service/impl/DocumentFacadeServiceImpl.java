package com.agentforge.service.impl;

import com.agentforge.converter.ChunkEmbeddingConverter;
import com.agentforge.converter.DocumentConverter;
import com.agentforge.exception.BizException;
import com.agentforge.mapper.ChunkEmbeddingMapper;
import com.agentforge.mapper.DocumentMapper;
import com.agentforge.mapper.KnowledgeBaseMapper;
import com.agentforge.model.dto.ChunkEmbeddingDTO;
import com.agentforge.model.dto.DocumentDTO;
import com.agentforge.model.entity.ChunkEmbedding;
import com.agentforge.model.entity.Document;
import com.agentforge.model.vo.DocumentVO;
import com.agentforge.rag.Splitter;
import com.agentforge.service.DocumentFacadeService;
import com.agentforge.service.DocumentStorageService;
import com.agentforge.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文档上传链路（api-contract.md §5）：
 * 校验 → 建 record → 落盘 → 标题分块 → 逐块向量化入库 → 回写 filePath。
 * 向量化失败整体回滚（删 record + 删文件）——半入库的知识库比失败更糟。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentFacadeServiceImpl implements DocumentFacadeService {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChunkEmbeddingMapper chunkEmbeddingMapper;
    private final DocumentConverter documentConverter;
    private final ChunkEmbeddingConverter chunkEmbeddingConverter;
    private final Splitter splitter;
    private final RagService ragService;
    private final DocumentStorageService storageService;

    @Override
    public List<DocumentVO> getDocumentsByKbId(String kbId) {
        if (knowledgeBaseMapper.selectById(kbId) == null) {
            throw new BizException("知识库不存在: " + kbId);
        }
        return documentMapper.selectByKbId(kbId).stream()
                .map(documentConverter::toDTO)
                .map(documentConverter::toVO)
                .toList();
    }

    @Override
    public void deleteDocument(String documentId) {
        Document existing = documentMapper.selectById(documentId);
        if (existing == null) {
            throw new BizException("文档不存在: " + documentId);
        }
        documentMapper.deleteById(documentId);   // chunk 经 FK CASCADE 连带删除
        storageService.deleteDirectory(existing.getKbId(), documentId);
    }

    @Override
    public String uploadDocument(String kbId, MultipartFile file) {
        if (knowledgeBaseMapper.selectById(kbId) == null) {
            throw new BizException("知识库不存在: " + kbId);
        }
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        String filename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!filename.toLowerCase().endsWith(".md")) {
            throw new BizException("仅支持 Markdown 文档（.md）");
        }

        // 1. 建记录拿 documentId
        DocumentDTO dto = DocumentDTO.builder()
                .kbId(kbId).filename(filename).filetype("md").size(file.getSize())
                .build();
        Document entity = documentConverter.toEntity(dto);
        documentMapper.insert(entity);

        try {
            // 2. 落盘 + 回写 filePath
            String filePath = storageService.saveFile(kbId, entity.getId(), file);
            DocumentDTO withPath = documentConverter.toDTO(entity);
            withPath.setMetadata(DocumentDTO.MetaData.builder().filePath(filePath).build());
            documentMapper.updateById(documentConverter.toEntity(withPath));

            // 3. 分块 + 向量化入库
            String content = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<Splitter.Section> sections = splitter.split(content);
            int index = 0;
            for (Splitter.Section section : sections) {
                String embedText = section.title() != null
                        ? section.title()
                        : section.content().substring(0, Math.min(256, section.content().length()));
                ChunkEmbeddingDTO chunk = ChunkEmbeddingDTO.builder()
                        .kbId(kbId).docId(entity.getId())
                        .content(section.content())
                        .metadata(ChunkEmbeddingDTO.MetaData.builder()
                                .chunkIndex(index++).title(section.title()).build())
                        .embedding(ragService.embed(embedText))
                        .build();
                ChunkEmbedding chunkEntity = chunkEmbeddingConverter.toEntity(chunk);
                chunkEmbeddingMapper.insert(chunkEntity);
            }
            log.info("文档入库完成 doc={} filename={} sections={}", entity.getId(), filename, sections.size());
            return entity.getId();
        } catch (Exception e) {
            // 整体回滚：留一个没有分块的文档会让检索静默漏数据
            documentMapper.deleteById(entity.getId());
            storageService.deleteDirectory(kbId, entity.getId());
            if (e instanceof BizException biz) {
                throw biz;
            }
            log.error("文档处理失败，已回滚 doc={}", entity.getId(), e);
            throw new BizException("文档处理失败: " + e.getMessage());
        }
    }
}
