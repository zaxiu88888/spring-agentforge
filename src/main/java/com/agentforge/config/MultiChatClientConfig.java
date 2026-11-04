package com.agentforge.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 多模型装配（tech-design.md §2 config）：
 * Bean 名 = 注册表 key = Agent.model 配置值，三者同字符串对齐。
 * 新增模型 = 这里加一个 @Bean + AgentDTO.ModelType 加一项，其余零改动。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MultiChatClientConfig {

    public static final String MODEL_GLM_4_6 = "glm-4.6";
    public static final String MODEL_DEEPSEEK_CHAT = "deepseek-chat";

    private final Environment environment;

    @Bean(MODEL_GLM_4_6)
    public ChatClient glmChatClient(ZhiPuAiChatModel zhiPuAiChatModel) {
        return ChatClient.create(zhiPuAiChatModel);
    }

    @Bean(MODEL_DEEPSEEK_CHAT)
    public ChatClient deepSeekChatClient(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.create(deepSeekChatModel);
    }

    /** M1 自审遗留项：哑 key 只保证 context 能加载，真正调用会失败——启动时显式告警，避免排查半天 */
    @PostConstruct
    void warnOnDummyApiKeys() {
        warnIfDummy("spring.ai.zhipuai.api-key", "ZHIPU_API_KEY", MODEL_GLM_4_6);
        warnIfDummy("spring.ai.deepseek.api-key", "DEEPSEEK_API_KEY", MODEL_DEEPSEEK_CHAT);
    }

    private void warnIfDummy(String property, String envName, String modelName) {
        String value = environment.getProperty(property);
        if (value != null && value.startsWith("dummy")) {
            log.warn("模型 [{}] 的 API key 未配置（{}），当前为哑默认值，AI 调用将失败——请设置环境变量 {}",
                    modelName, property, envName);
        }
    }
}
