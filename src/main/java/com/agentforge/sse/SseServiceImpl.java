package com.agentforge.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SseEmitter 连接池：chatSessionId → emitter，ConcurrentHashMap 管理生命周期。
 * 30 分钟超时；同会话重复连接以最新为准（旧连接完成清理）。
 */
@Slf4j
@Service
public class SseServiceImpl implements SseService {

    public static final long EMITTER_TIMEOUT = 30L * 60 * 1000;

    private final Map<String, SseEmitter> clients = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SseServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter connect(String chatSessionId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        // 同会话旧连接先完成掉，避免双连接各收一份
        SseEmitter previous = clients.put(chatSessionId, emitter);
        if (previous != null) {
            previous.complete();
        }
        emitter.onCompletion(() -> clients.remove(chatSessionId, emitter));
        emitter.onTimeout(() -> {
            clients.remove(chatSessionId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> clients.remove(chatSessionId, emitter));

        try {
            emitter.send(SseEmitter.event().name("init").data("connected"));
        } catch (IOException e) {
            clients.remove(chatSessionId, emitter);
            log.warn("SSE init 发送失败 session={}", chatSessionId, e);
        }
        log.debug("SSE 已连接 session={}（当前连接数={}）", chatSessionId, clients.size());
        return emitter;
    }

    @Override
    public boolean send(String chatSessionId, SseMessage message) {
        SseEmitter emitter = clients.get(chatSessionId);
        if (emitter == null) {
            // 🩹 原型在此抛 RuntimeException：前端未连 SSE 就发消息 → Agent 异常终止。
            // 消息已落库，跳过推送即可，前端拉历史对账。
            log.debug("SSE 无连接，跳过推送 session={} type={}", chatSessionId, message.type());
            return false;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            emitter.send(SseEmitter.event().name("message").data(json));
            return true;
        } catch (IOException | IllegalStateException e) {
            // 连接已死（浏览器关页/网络断）：清理后按无连接处理，不向上抛
            clients.remove(chatSessionId, emitter);
            log.debug("SSE 推送失败（连接失效），已清理 session={}", chatSessionId);
            return false;
        }
    }

    @Override
    public void disconnect(String chatSessionId) {
        SseEmitter emitter = clients.remove(chatSessionId);
        if (emitter != null) {
            emitter.complete();
        }
    }
}
