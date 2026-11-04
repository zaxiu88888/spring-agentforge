package com.agentforge.service.impl;

import com.agentforge.converter.KnowledgeBaseConverter;
import com.agentforge.exception.BizException;
import com.agentforge.mapper.KnowledgeBaseMapper;
import com.agentforge.model.dto.KnowledgeBaseDTO;
import com.agentforge.model.entity.KnowledgeBase;
import com.agentforge.model.request.KnowledgeBaseRequests.CreateKnowledgeBaseRequest;
import com.agentforge.model.request.KnowledgeBaseRequests.UpdateKnowledgeBaseRequest;
import com.agentforge.model.vo.KnowledgeBaseVO;
import com.agentforge.service.KnowledgeBaseFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseFacadeServiceImpl implements KnowledgeBaseFacadeService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseConverter knowledgeBaseConverter;

    @Override
    public String createKnowledgeBase(CreateKnowledgeBaseRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("知识库名称不能为空");
        }
        KnowledgeBaseDTO dto = knowledgeBaseConverter.toDTO(request);
        KnowledgeBase entity = knowledgeBaseConverter.toEntity(dto);
        knowledgeBaseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public List<KnowledgeBaseVO> getKnowledgeBases() {
        return knowledgeBaseMapper.selectAll().stream()
                .map(knowledgeBaseConverter::toDTO)
                .map(knowledgeBaseConverter::toVO)
                .toList();
    }

    @Override
    public void updateKnowledgeBase(String knowledgeBaseId, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase existing = requireKb(knowledgeBaseId);
        KnowledgeBaseDTO dto = knowledgeBaseConverter.toDTO(existing);
        knowledgeBaseConverter.updateDTOFromRequest(dto, request);
        KnowledgeBase entity = knowledgeBaseConverter.toEntity(dto);
        entity.setId(knowledgeBaseId);
        knowledgeBaseMapper.updateById(entity);
    }

    @Override
    public void deleteKnowledgeBase(String knowledgeBaseId) {
        requireKb(knowledgeBaseId);
        knowledgeBaseMapper.deleteById(knowledgeBaseId);   // document/chunk 经 FK CASCADE 连带删除
    }

    private KnowledgeBase requireKb(String knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null) {
            throw new BizException("知识库不存在: " + knowledgeBaseId);
        }
        return kb;
    }
}
