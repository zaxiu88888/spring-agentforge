package com.agentforge.service;

import com.agentforge.model.request.KnowledgeBaseRequests.CreateKnowledgeBaseRequest;
import com.agentforge.model.request.KnowledgeBaseRequests.UpdateKnowledgeBaseRequest;
import com.agentforge.model.vo.KnowledgeBaseVO;

import java.util.List;

public interface KnowledgeBaseFacadeService {

    String createKnowledgeBase(CreateKnowledgeBaseRequest request);

    List<KnowledgeBaseVO> getKnowledgeBases();

    void updateKnowledgeBase(String knowledgeBaseId, UpdateKnowledgeBaseRequest request);

    void deleteKnowledgeBase(String knowledgeBaseId);
}
