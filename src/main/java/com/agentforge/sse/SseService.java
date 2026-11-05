package com.agentforge.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** SSE 连接管理（tech-design.md §3.3） */
public interface SseService {

    /** 建立会话连接；返回的 emitter 已发送 init 事件 */
    SseEmitter connect(String chatSessionId);

    /**
     * 向会话推送业务事件。
     * 🩹 与原型的关键差异：找不到连接【不抛异常】——消息已落库，
     * 前端重连后拉历史即可对账；这里只记 debug 日志。
     *
     * @return true=已送达；false=无连接（跳过）
     */
    boolean send(String chatSessionId, SseMessage message);

    void disconnect(String chatSessionId);
}
