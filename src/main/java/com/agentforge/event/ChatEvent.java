package com.agentforge.event;

/**
 * 用户消息事件：POST /chat-messages 落库后发布，@Async 监听器据此装配并运行 Agent（api-contract.md §3）。
 */
public record ChatEvent(String agentId, String sessionId, String userInput) {
}
