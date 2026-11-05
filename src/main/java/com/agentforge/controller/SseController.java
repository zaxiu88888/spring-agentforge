package com.agentforge.controller;

import com.agentforge.sse.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** GET /sse/connect/{chatSessionId}（api-contract.md §8，注意无 /api 前缀） */
@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @GetMapping(path = "/sse/connect/{chatSessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String chatSessionId) {
        return sseService.connect(chatSessionId);
    }
}
