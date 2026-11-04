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
public class ChatSession {
    private String id;
    private String agentId;
    private String title;
    private String metadata;       // JSON，预留
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
