package com.agentforge.model.vo;

import com.agentforge.model.dto.AgentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVO {
    private String id;
    private String name;
    private String description;
    private String systemPrompt;
    private AgentDTO.ModelType model;
    private List<String> allowedTools;
    private List<String> allowedKbs;
    private AgentDTO.ChatOptions chatOptions;
}
