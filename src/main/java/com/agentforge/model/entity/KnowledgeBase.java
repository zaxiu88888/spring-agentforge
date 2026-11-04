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
public class KnowledgeBase {
    private String id;
    private String name;
    private String description;
    private String metadata;       // JSON，预留
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
