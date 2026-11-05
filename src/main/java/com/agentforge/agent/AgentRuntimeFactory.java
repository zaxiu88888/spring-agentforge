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
import com.agentforge.model.vo.ChatMessageVO;
import com.agentforge.service.ChatMessageFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agent 运行时装配（tech-design.md §2 agent/）：
 * 加载配置 → 回放记忆 → 解析工具（FIXED 全量 + OPTIONAL 按名） → 模型路由 → 组装 AgentLoop。
 *
 * 🩹 与原型的关键差异：全部装配状态走局部变量按值传递——
 * 原型 Factory 是单例却持有实例字段 agentConfig，并发创建多个 Agent 时互相覆盖（共享状态缺陷）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRuntimeFactory {

    public static final int DEFAULT_MEMORY_WINDOW = 10;

    private final AgentMapper agentMapper;
    private final AgentConverter agentConverter;
    private final ChatMessageConverter chatMessageConverter;
    private final ChatMessageFacadeService chatMessageFacadeService;
    private final com.agentforge.mapper.KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChatClientRegistry chatClientRegistry;
    private final List<AgentTool> registeredTools;
    private final AgentProgressListener progressListener;

    public AgentLoop create(String agentId, String sessionId) {
        // 1. 加载 Agent 配置（局部变量，线程安全）
        Agent entity = agentMapper.selectById(agentId);
        if (entity == null) {
            throw new BizException("Agent 不存在: " + agentId);
        }
        AgentDTO config = agentConverter.toDTO(entity);
        if (config.getModel() == null) {
            throw new BizException("Agent 未配置模型: " + agentId);
        }

        // 2. 记忆窗口与回放（系统提示词由配置重建，不依赖 DB）
        int window = resolveWindow(config);
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(window)
                .build();
        chatMemory.add(sessionId, restoreHistory(sessionId, config.getSystemPrompt(), window));

        // 3. 工具装配：FIXED 全量 + allowedTools 命中的 OPTIONAL
        List<ToolCallback> toolCallbacks = buildToolCallbacks(resolveTools(config.getAllowedTools()));

        // 4. 知识库提示（knowledgeQuery 工具的 kbId 参数来源）
        String knowledgeHint = buildKnowledgeHint(resolveKnowledgeBases(config.getAllowedKbs()));

        // 5. 模型路由 + 消息下沉通道 + 组装
        ChatClient chatClient = chatClientRegistry.require(config.getModel().getModelName());
        AgentMessageSink sink = dto -> {
            String id = chatMessageFacadeService.agentCreateChatMessage(dto);
            ChatMessageVO vo = chatMessageConverter.toVO(dto);
            vo.setId(id);
            return vo;
        };

        log.info("Agent 装配完成 agent={} model={} tools={} memoryWindow={}",
                agentId, config.getModel().getModelName(),
                toolCallbacks.stream().map(cb -> cb.getToolDefinition().name()).toList(), window);
        return new AgentLoop(chatClient, toolCallbacks, chatMemory, sessionId,
                AgentLoop.DEFAULT_MAX_STEPS, sink, progressListener, knowledgeHint);
    }

    /** 知识库提示串：name(kbId=...) 分号分隔，注入 think 提示词（M6） */
    static String buildKnowledgeHint(List<com.agentforge.model.entity.KnowledgeBase> kbs) {
        if (kbs == null || kbs.isEmpty()) {
            return "";
        }
        return kbs.stream()
                .map(kb -> kb.getName() + "(kbId=" + kb.getId() + ")")
                .collect(Collectors.joining("; "));
    }

    private List<com.agentforge.model.entity.KnowledgeBase> resolveKnowledgeBases(List<String> allowedKbs) {
        if (allowedKbs == null || allowedKbs.isEmpty()) {
            return List.of();
        }
        return knowledgeBaseMapper.selectByIdBatch(allowedKbs);
    }

    private int resolveWindow(AgentDTO config) {
        if (config.getChatOptions() == null || config.getChatOptions().getMessageLength() == null) {
            return DEFAULT_MEMORY_WINDOW;
        }
        return Math.max(1, config.getChatOptions().getMessageLength());
    }

    /** DB 消息按角色还原为 Spring AI Message（记忆持久化协议，tech-design.md §3.2） */
    private List<Message> restoreHistory(String sessionId, String systemPrompt, int window) {
        List<ChatMessageDTO> recent = chatMessageFacadeService.getRecentMessages(sessionId, window);
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        for (ChatMessageDTO dto : recent) {
            switch (dto.getRole()) {
                case USER -> messages.add(new UserMessage(dto.getContent()));
                case ASSISTANT -> messages.add(AssistantMessage.builder()
                        .content(dto.getContent())
                        .toolCalls(dto.getMetadata() == null ? null : dto.getMetadata().getToolCalls())
                        .build());
                case TOOL -> {
                    if (dto.getMetadata() != null && dto.getMetadata().getToolResponse() != null) {
                        messages.add(ToolResponseMessage.builder()
                                .responses(List.of(dto.getMetadata().getToolResponse()))
                                .build());
                    }
                }
                case SYSTEM -> { /* 系统提示词由 Agent 配置重建，DB 中的不重复回放 */ }
            }
        }
        return messages;
    }

    private List<AgentTool> resolveTools(List<String> allowedTools) {
        List<AgentTool> fixed = registeredTools.stream()
                .filter(t -> t.getType() == ToolType.FIXED)
                .toList();
        if (allowedTools == null || allowedTools.isEmpty()) {
            return fixed;
        }
        Map<String, AgentTool> optionalByName = registeredTools.stream()
                .filter(t -> t.getType() == ToolType.OPTIONAL)
                .collect(Collectors.toMap(AgentTool::getName, Function.identity(), (a, b) -> a));
        List<AgentTool> merged = new ArrayList<>(fixed);
        allowedTools.stream()
                .map(optionalByName::get)
                .filter(t -> t != null)
                .forEach(merged::add);
        return merged;
    }

    /** MethodToolCallbackProvider 不接受 AOP 代理对象：先解包拿原始 Bean */
    private List<ToolCallback> buildToolCallbacks(List<AgentTool> tools) {
        List<ToolCallback> callbacks = new ArrayList<>();
        for (AgentTool tool : tools) {
            Object target = unwrapProxy(tool);
            ToolCallback[] found = MethodToolCallbackProvider.builder()
                    .toolObjects(target)
                    .build()
                    .getToolCallbacks();
            callbacks.addAll(List.of(found));
        }
        return callbacks;
    }

    private Object unwrapProxy(Object bean) {
        if (AopUtils.isAopProxy(bean) && bean instanceof org.springframework.aop.framework.Advised advised) {
            try {
                return advised.getTargetSource().getTarget();
            } catch (Exception e) {
                log.warn("解包 AOP 代理失败，退回代理对象: {}", bean.getClass(), e);
            }
        }
        return bean;
    }
}
