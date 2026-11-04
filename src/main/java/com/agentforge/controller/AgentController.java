package com.agentforge.controller;

import com.agentforge.model.common.ApiResponse;
import com.agentforge.model.request.AgentRequests.CreateAgentRequest;
import com.agentforge.model.request.AgentRequests.UpdateAgentRequest;
import com.agentforge.model.response.Responses.CreateAgentResponse;
import com.agentforge.model.response.Responses.GetAgentsResponse;
import com.agentforge.service.AgentFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AgentController {

    private final AgentFacadeService agentFacadeService;

    @GetMapping("/agents")
    public ApiResponse<GetAgentsResponse> getAgents() {
        return ApiResponse.success(GetAgentsResponse.builder()
                .agents(agentFacadeService.getAgents())
                .build());
    }

    @PostMapping("/agents")
    public ApiResponse<CreateAgentResponse> createAgent(@RequestBody CreateAgentRequest request) {
        return ApiResponse.success(CreateAgentResponse.builder()
                .agentId(agentFacadeService.createAgent(request))
                .build());
    }

    @PatchMapping("/agents/{agentId}")
    public ApiResponse<Void> updateAgent(@PathVariable String agentId,
                                         @RequestBody UpdateAgentRequest request) {
        agentFacadeService.updateAgent(agentId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/agents/{agentId}")
    public ApiResponse<Void> deleteAgent(@PathVariable String agentId) {
        agentFacadeService.deleteAgent(agentId);
        return ApiResponse.success();
    }
}
