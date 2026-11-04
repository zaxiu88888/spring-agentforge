package com.agentforge.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String id;
    private String sessionId;
    private String role;           // user / assistant / system / tool
    private String content;
    private String metadata;       // JSON: {toolCalls:[...], toolResponse:{...}}
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
