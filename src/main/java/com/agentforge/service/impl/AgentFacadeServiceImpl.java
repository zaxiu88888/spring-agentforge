package com.agentforge.service.impl;

import com.agentforge.converter.AgentConverter;
import com.agentforge.exception.BizException;
import com.agentforge.mapper.AgentMapper;
import com.agentforge.model.dto.AgentDTO;
import com.agentforge.model.entity.Agent;
import com.agentforge.model.request.AgentRequests.CreateAgentRequest;
import com.agentforge.model.request.AgentRequests.UpdateAgentRequest;
import com.agentforge.model.vo.AgentVO;
import com.agentforge.service.AgentFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentFacadeServiceImpl implements AgentFacadeService {

    private final AgentMapper agentMapper;
    private final AgentConverter agentConverter;

    @Override
    public String createAgent(CreateAgentRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("Agent 名称不能为空");
        }
        if (!StringUtils.hasText(request.getModel())) {
            throw new BizException("必须选择模型");
        }
        AgentDTO dto = agentConverter.toDTO(request);
        Agent entity = agentConverter.toEntity(dto);
        agentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public List<AgentVO> getAgents() {
        return agentMapper.selectAll().stream()
                .map(agentConverter::toDTO)
                .map(agentConverter::toVO)
                .toList();
    }

    @Override
    public void updateAgent(String agentId, UpdateAgentRequest request) {
        Agent existing = requireAgent(agentId);
        AgentDTO dto = agentConverter.toDTO(existing);
        agentConverter.updateDTOFromRequest(dto, request);
        Agent entity = agentConverter.toEntity(dto);
        entity.setId(agentId);
        agentMapper.updateById(entity);
    }

    @Override
    public void deleteAgent(String agentId) {
        requireAgent(agentId);
        agentMapper.deleteById(agentId);
    }

    private Agent requireAgent(String agentId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException("Agent 不存在: " + agentId);
        }
        return agent;
    }
}
