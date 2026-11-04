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
public class Document {
    private String id;
    private String kbId;
    private String filename;
    private String filetype;       // md / pdf / txt
    private Long size;
    private String metadata;       // JSON: {filePath: ...}
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
