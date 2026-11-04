package com.agentforge.controller;

import com.agentforge.model.common.ApiResponse;
import com.agentforge.model.request.CreateChatMessageRequest;
import com.agentforge.model.response.Responses.CreateChatMessageResponse;
import com.agentforge.model.response.Responses.GetChatMessagesResponse;
import com.agentforge.service.ChatMessageFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageFacadeService chatMessageFacadeService;

    @GetMapping("/chat-messages/session/{sessionId}")
    public ApiResponse<GetChatMessagesResponse> getChatMessages(@PathVariable String sessionId) {
        return ApiResponse.success(GetChatMessagesResponse.builder()
                .chatMessages(chatMessageFacadeService.getChatMessagesBySessionId(sessionId))
                .build());
    }

    /** Agent 触发入口：M2 纯落库；M4 起落库后发布 ChatEvent 异步执行 Agent（api-contract.md §3） */
    @PostMapping("/chat-messages")
    public ApiResponse<CreateChatMessageResponse> createChatMessage(@RequestBody CreateChatMessageRequest request) {
        return ApiResponse.success(CreateChatMessageResponse.builder()
                .chatMessageId(chatMessageFacadeService.createChatMessage(request))
                .build());
    }
}
