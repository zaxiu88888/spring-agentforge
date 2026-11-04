package com.agentforge.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** JSONB 列在 Entity 层统一为 JSON 字符串，由 Converter 与 DTO 强类型互转（tech-design.md §5） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent {
    private String id;
    private String name;
    private String description;
    private String systemPrompt;
    private String model;
    private String allowedTools;   // JSON: ["sqlQuery", ...]
    private String allowedKbs;     // JSON: ["<kb-uuid>", ...]
    private String chatOptions;    // JSON: {temperature, topP, messageLength}
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
