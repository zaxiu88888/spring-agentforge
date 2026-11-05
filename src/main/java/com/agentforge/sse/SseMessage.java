package com.agentforge.sse;

import com.agentforge.model.vo.ChatMessageVO;

/**
 * SSE 事件协议（api-contract.md §8，4 种类型全部真实实现）。
 * 具名事件 message 的 data 即本记录的 JSON。
 */
public record SseMessage(Type type, Payload payload, Metadata metadata) {

    public enum Type {
        AI_THINKING,
        AI_EXECUTING,
        AI_GENERATED_CONTENT,
        AI_DONE
    }

    public record Payload(ChatMessageVO message, String statusText, boolean done) {
    }

    public record Metadata(String chatMessageId) {
    }

    // ---- 工厂：四类事件的构造 ----

    public static SseMessage thinking(String statusText) {
        return new SseMessage(Type.AI_THINKING, new Payload(null, statusText, false), null);
    }

    public static SseMessage executing(String statusText) {
        return new SseMessage(Type.AI_EXECUTING, new Payload(null, statusText, false), null);
    }

    public static SseMessage generated(ChatMessageVO message) {
        return new SseMessage(Type.AI_GENERATED_CONTENT,
                new Payload(message, null, false),
                new Metadata(message.getId()));
    }

    public static SseMessage done(String errorSummary) {
        return new SseMessage(Type.AI_DONE,
                new Payload(null, errorSummary, true), null);
    }
}
