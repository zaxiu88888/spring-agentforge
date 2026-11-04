package com.agentforge.config;

import com.agentforge.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 多模型注册表：利用 Spring「集合注入按 Bean 名为 key」的特性，
 * 容器中所有 ChatClient Bean 自动汇入（Bean 名即模型名）。
 * Agent 运行时按 Agent.model 值经 require() 取用。
 */
@Slf4j
@Component
public class ChatClientRegistry {

    private final Map<String, ChatClient> chatClients;

    public ChatClientRegistry(Map<String, ChatClient> chatClients) {
        this.chatClients = Map.copyOf(chatClients);
        log.info("ChatClientRegistry 已注册模型: {}", chatClients.keySet());
    }

    /** 按模型名取 ChatClient；未注册返回 null */
    public ChatClient get(String modelName) {
        return chatClients.get(modelName);
    }

    /** 按模型名取 ChatClient；未注册抛业务异常（Agent 装配用，错误信息直达前端） */
    public ChatClient require(String modelName) {
        ChatClient client = chatClients.get(modelName);
        if (client == null) {
            throw new BizException("未注册的模型: " + modelName + "，可用模型: " + chatClients.keySet());
        }
        return client;
    }

    public Set<String> registeredModels() {
        return chatClients.keySet();
    }
}
