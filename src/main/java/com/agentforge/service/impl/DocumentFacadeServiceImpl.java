package com.agentforge.service.impl;

import com.agentforge.converter.DocumentConverter;
import com.agentforge.exception.BizException;
import com.agentforge.mapper.DocumentMapper;
import com.agentforge.mapper.KnowledgeBaseMapper;
import com.agentforge.model.vo.DocumentVO;
import com.agentforge.service.DocumentFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentFacadeServiceImpl implements DocumentFacadeService {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentConverter documentConverter;

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
        if (documentMapper.selectById(documentId) == null) {
            throw new BizException("文档不存在: " + documentId);
        }
        documentMapper.deleteById(documentId);   // chunk_embedding 经 FK CASCADE 连带删除；文件清理 M6 补
    }

    @Override
    public String uploadDocument(String kbId, MultipartFile file) {
        throw new BizException("文档上传将在 RAG 里程碑（M6）开放");
    }
}
