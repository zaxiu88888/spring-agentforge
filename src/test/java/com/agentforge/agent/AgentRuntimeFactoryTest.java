package com.agentforge.agent;

import com.agentforge.agent.tools.AgentTool;
import com.agentforge.agent.tools.ToolType;
import com.agentforge.config.ChatClientRegistry;
import com.agentforge.converter.AgentConverter;
import com.agentforge.converter.ChatMessageConverter;
import com.agentforge.exception.BizException;
import com.agentforge.mapper.AgentMapper;
import com.agentforge.model.dto.AgentDTO;
import com.agentforge.model.dto.ChatMessageDTO;
import com.agentforge.model.entity.Agent;
import com.agentforge.service.ChatMessageFacadeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 装配逻辑单测：FIXED 必装 / OPTIONAL 按名 / 模型路由 / 异常语义（全 mock，不碰容器与 DB） */
class AgentRuntimeFactoryTest {

    // OPTIONAL 测试工具
    static class FakeOptionalTool implements AgentTool {
        public String getName() { return "fakeOptional"; }
        public String getDescription() { return "可选工具"; }
        public ToolType getType() { return ToolType.OPTIONAL; }

        @org.springframework.ai.tool.annotation.Tool(name = "fakeOptional", description = "可选")
        public String run() { return "ok"; }
    }

    static class FakeFixedTool implements com.agentforge.agent.tools.AgentTool {
        public String getName() { return "fakeFixed"; }
        public String getDescription() { return "固定工具"; }
        public ToolType getType() { return ToolType.FIXED; }

        @org.springframework.ai.tool.annotation.Tool(name = "fakeFixed", description = "固定")
        public String run() { return "ok"; }
    }

    private final AgentMapper agentMapper = mock(AgentMapper.class);
    private final AgentConverter agentConverter = mock(AgentConverter.class);
    private final ChatMessageConverter chatMessageConverter = mock(ChatMessageConverter.class);
    private final ChatMessageFacadeService messageService = mock(ChatMessageFacadeService.class);
    private final com.agentforge.mapper.KnowledgeBaseMapper knowledgeBaseMapper =
            mock(com.agentforge.mapper.KnowledgeBaseMapper.class);
    private final ChatClientRegistry registry = mock(ChatClientRegistry.class);
    private final AgentProgressListener progress = mock(AgentProgressListener.class);

    private final AgentRuntimeFactory factory = new AgentRuntimeFactory(
            agentMapper, agentConverter, chatMessageConverter, messageService, knowledgeBaseMapper,
            registry, List.of(new FakeFixedTool(), new FakeOptionalTool()), progress);

    private void stubAgent(String model, List<String> allowedTools) {
        Agent entity = Agent.builder().id("a1").name("t").model(model).build();
        when(agentMapper.selectById("a1")).thenReturn(entity);
        when(agentConverter.toDTO(entity)).thenReturn(AgentDTO.builder()
                .id("a1").name("t")
                .model(model == null ? null : AgentDTO.ModelType.fromModelName(model))
                .allowedTools(allowedTools)
                .chatOptions(AgentDTO.ChatOptions.defaultOptions())
                .build());
        when(registry.require(anyString())).thenReturn(mock(ChatClient.class));
        when(messageService.getRecentMessages(anyString(), anyInt())).thenReturn(List.of(
                ChatMessageDTO.builder().role(ChatMessageDTO.RoleType.USER).content("hi").build()));
    }

    @Test
    @DisplayName("FIXED 全量装配 + 命中的 OPTIONAL；未勾选的不装")
    void toolResolution() {
        stubAgent("glm-4.6", List.of("fakeOptional"));

        AgentLoop loop = factory.create("a1", "s1");

        assertThat(loop.toolNames()).containsExactlyInAnyOrder("fakeFixed", "fakeOptional");
    }

    @Test
    @DisplayName("未勾选任何 OPTIONAL 时仅装 FIXED")
    void onlyFixedWhenNoAllowlist() {
        stubAgent("glm-4.6", null);

        AgentLoop loop = factory.create("a1", "s1");

        assertThat(loop.toolNames()).containsExactly("fakeFixed");
    }

    @Test
    @DisplayName("allowlist 里的未知工具名被忽略（不抛错）")
    void unknownToolNameIgnored() {
        stubAgent("glm-4.6", List.of("no-such-tool"));

        AgentLoop loop = factory.create("a1", "s1");

        assertThat(loop.toolNames()).containsExactly("fakeFixed");
    }

    @Test
    @DisplayName("Agent 不存在 → BizException")
    void unknownAgentThrows() {
        when(agentMapper.selectById(anyString())).thenReturn(null);
        assertThatThrownBy(() -> factory.create("ghost", "s1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Agent 不存在");
    }

    @Test
    @DisplayName("模型未配置 → BizException")
    void missingModelThrows() {
        stubAgent(null, null);
        assertThatThrownBy(() -> factory.create("a1", "s1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未配置模型");
    }

    @Test
    @DisplayName("知识库提示串：name(kbId=...) 分号分隔；空列表返回空串")
    void knowledgeHintFormat() {
        assertThat(AgentRuntimeFactory.buildKnowledgeHint(null)).isEmpty();
        assertThat(AgentRuntimeFactory.buildKnowledgeHint(List.of())).isEmpty();
        assertThat(AgentRuntimeFactory.buildKnowledgeHint(List.of(
                com.agentforge.model.entity.KnowledgeBase.builder()
                        .id("11111111-1111-1111-1111-111111111111").name("产品手册").build(),
                com.agentforge.model.entity.KnowledgeBase.builder()
                        .id("22222222-2222-2222-2222-222222222222").name("帮助中心").build())))
                .isEqualTo("产品手册(kbId=11111111-1111-1111-1111-111111111111); "
                        + "帮助中心(kbId=22222222-2222-2222-2222-222222222222)");
    }

    @Test
    @DisplayName("AgentLoop 不可重入：run 后再 run 抛 IllegalStateException")
    void loopNotReentrant() {
        stubAgent("glm-4.6", null);
        // 深桩：一次调用直接给出无工具回答，run 立即 FINISHED
        ChatClient client = mock(ChatClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(client.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))
                .system(any(String.class))
                .toolCallbacks(any(org.springframework.ai.tool.ToolCallback[].class))
                .call()
                .chatClientResponse()
                .chatResponse())
                .thenReturn(org.mockito.Mockito.mock(org.springframework.ai.chat.model.ChatResponse.class,
                        org.mockito.Mockito.RETURNS_DEEP_STUBS));
        when(registry.require("glm-4.6")).thenReturn(client);
        AgentLoop loop = factory.create("a1", "s1");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            loop.run();   // 深桩 ChatResponse 的 getOutput() 返回 null → 路径6 → ERROR，run 正常返回
            loop.run();   // 第二次 run：状态非 IDLE → IllegalStateException
        }).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不可重入");
    }
}
