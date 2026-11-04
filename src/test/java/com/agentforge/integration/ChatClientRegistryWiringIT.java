package com.agentforge.integration;

import com.agentforge.config.ChatClientRegistry;
import com.agentforge.model.dto.AgentDTO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 装配验证：Bean 名 = 注册表 key = AgentDTO.ModelType 枚举值，三方逐字对齐。
 * 任何一方改名，此测试立刻红——这是多模型设计的关键不变量。
 */
@Tag("integration")
@SpringBootTest
class ChatClientRegistryWiringIT {

    @Autowired
    private ChatClientRegistry registry;

    @Test
    void bothModelsRegistered() {
        assertThat(registry.registeredModels())
                .containsExactlyInAnyOrder("glm-4.6", "deepseek-chat");
    }

    @Test
    void everyModelTypeEnumValueHasAChatClient() {
        for (AgentDTO.ModelType type : AgentDTO.ModelType.values()) {
            ChatClient client = registry.get(type.getModelName());
            assertThat(client)
                    .as("ModelType [%s] 必须有同名 ChatClient Bean", type.getModelName())
                    .isNotNull();
        }
    }
}
