package com.agentforge.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.List;

/** 字段与 api-contract.md §0 的 TS 类型逐字一致 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {
    private String id;
    private String sessionId;
    private String role;           // user / assistant / system / tool
    private String content;
    private MetaData metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaData {
        private List<AssistantMessage.ToolCall> toolCalls;
        private ToolResponseMessage.ToolResponse toolResponse;
    }
}
