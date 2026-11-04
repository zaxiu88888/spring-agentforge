package com.agentforge.service;

import com.agentforge.model.vo.DocumentVO;

import java.util.List;

public interface DocumentFacadeService {

    List<DocumentVO> getDocumentsByKbId(String kbId);

    void deleteDocument(String documentId);

    /** md 上传 + 分块 + 向量化（M6 RAG 里程碑实现，api-contract.md §5） */
    String uploadDocument(String kbId, org.springframework.web.multipart.MultipartFile file);
}
