package com.agentforge.service;

import com.agentforge.model.request.AgentRequests.CreateAgentRequest;
import com.agentforge.model.request.AgentRequests.UpdateAgentRequest;
import com.agentforge.model.vo.AgentVO;

import java.util.List;

public interface AgentFacadeService {

    String createAgent(CreateAgentRequest request);

    List<AgentVO> getAgents();

    void updateAgent(String agentId, UpdateAgentRequest request);

    void deleteAgent(String agentId);
}
