package com.agentforge.config;

import com.agentforge.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 注册表语义（纯单测，不启 Spring 容器） */
class ChatClientRegistryTest {

    private final ChatClient stubA = org.mockito.Mockito.mock(ChatClient.class);
    private final ChatClient stubB = org.mockito.Mockito.mock(ChatClient.class);

    private ChatClientRegistry registryWith(ChatClient a, ChatClient b) {
        Map<String, ChatClient> map = new HashMap<>();
        map.put("glm-4.6", a);
        map.put("deepseek-chat", b);
        return new ChatClientRegistry(map);
    }

    @Test
    @DisplayName("get 按名取用；未注册返回 null")
    void getByName() {
        ChatClientRegistry registry = registryWith(stubA, stubB);
        assertThat(registry.get("glm-4.6")).isSameAs(stubA);
        assertThat(registry.get("deepseek-chat")).isSameAs(stubB);
        assertThat(registry.get("qwen-max")).isNull();
    }

    @Test
    @DisplayName("require 未注册时抛业务异常且消息列出可用模型")
    void requireUnknownThrows() {
        ChatClientRegistry registry = registryWith(stubA, stubB);
        assertThatThrownBy(() -> registry.require("qwen-max"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未注册的模型: qwen-max")
                .hasMessageContaining("glm-4.6")
                .hasMessageContaining("deepseek-chat");
    }

    @Test
    @DisplayName("registeredModels 返回全部模型名")
    void listsModels() {
        ChatClientRegistry registry = registryWith(stubA, stubB);
        assertThat(registry.registeredModels()).containsExactlyInAnyOrder("glm-4.6", "deepseek-chat");
    }
}
