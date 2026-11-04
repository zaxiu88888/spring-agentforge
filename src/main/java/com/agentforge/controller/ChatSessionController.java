package com.agentforge.controller;

import com.agentforge.model.common.ApiResponse;
import com.agentforge.model.request.ChatSessionRequests.CreateChatSessionRequest;
import com.agentforge.model.request.ChatSessionRequests.UpdateChatSessionRequest;
import com.agentforge.model.response.Responses.CreateChatSessionResponse;
import com.agentforge.model.response.Responses.GetChatSessionsResponse;
import com.agentforge.service.ChatSessionFacadeService;
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
public class ChatSessionController {

    private final ChatSessionFacadeService chatSessionFacadeService;

    @GetMapping("/chat-sessions")
    public ApiResponse<GetChatSessionsResponse> getChatSessions() {
        return ApiResponse.success(GetChatSessionsResponse.builder()
                .chatSessions(chatSessionFacadeService.getChatSessions())
                .build());
    }

    @GetMapping("/chat-sessions/agent/{agentId}")
    public ApiResponse<GetChatSessionsResponse> getChatSessionsByAgentId(@PathVariable String agentId) {
        return ApiResponse.success(GetChatSessionsResponse.builder()
                .chatSessions(chatSessionFacadeService.getChatSessionsByAgentId(agentId))
                .build());
    }

    @PostMapping("/chat-sessions")
    public ApiResponse<CreateChatSessionResponse> createChatSession(@RequestBody CreateChatSessionRequest request) {
        return ApiResponse.success(CreateChatSessionResponse.builder()
                .chatSessionId(chatSessionFacadeService.createChatSession(request))
                .build());
    }

    @PatchMapping("/chat-sessions/{chatSessionId}")
    public ApiResponse<Void> updateChatSession(@PathVariable String chatSessionId,
                                               @RequestBody UpdateChatSessionRequest request) {
        chatSessionFacadeService.updateChatSession(chatSessionId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/chat-sessions/{chatSessionId}")
    public ApiResponse<Void> deleteChatSession(@PathVariable String chatSessionId) {
        chatSessionFacadeService.deleteChatSession(chatSessionId);
        return ApiResponse.success();
    }
}
