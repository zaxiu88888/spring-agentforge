package com.agentforge.service.impl;

import com.agentforge.agent.tools.AgentTool;
import com.agentforge.agent.tools.ToolType;
import com.agentforge.model.vo.ToolVO;
import com.agentforge.service.ToolFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolFacadeServiceImpl implements ToolFacadeService {

    /** Spring 集合注入：容器中所有 AgentTool Bean */
    private final List<AgentTool> registeredTools;

    @Override
    public List<ToolVO> getOptionalTools() {
        return registeredTools.stream()
                .filter(t -> t.getType() == ToolType.OPTIONAL)
                .map(t -> new ToolVO(t.getName(), t.getDescription(), t.getType().name()))
                .toList();
    }
}
