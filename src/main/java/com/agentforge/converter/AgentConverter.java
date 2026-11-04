package com.agentforge.converter;

import com.agentforge.model.dto.AgentDTO;
import com.agentforge.model.entity.Agent;
import com.agentforge.model.request.AgentRequests.CreateAgentRequest;
import com.agentforge.model.request.AgentRequests.UpdateAgentRequest;
import com.agentforge.model.vo.AgentVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentConverter {

    private final ObjectMapper objectMapper;

    public AgentDTO toDTO(Agent entity) {
        Assert.notNull(entity, "entity 不能为空");
        return AgentDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .systemPrompt(entity.getSystemPrompt())
                .model(entity.getModel() == null ? null : AgentDTO.ModelType.fromModelName(entity.getModel()))
                .allowedTools(fromJsonList(entity.getAllowedTools()))
                .allowedKbs(fromJsonList(entity.getAllowedKbs()))
                .chatOptions(fromJson(entity.getChatOptions(), AgentDTO.ChatOptions.class))
                .build();
    }

    public Agent toEntity(AgentDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        return Agent.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .systemPrompt(dto.getSystemPrompt())
                .model(dto.getModel() == null ? null : dto.getModel().getModelName())
                .allowedTools(toJsonList(dto.getAllowedTools()))
                .allowedKbs(toJsonList(dto.getAllowedKbs()))
                .chatOptions(toJson(dto.getChatOptions()))
                .build();
    }

    public AgentDTO toDTO(CreateAgentRequest request) {
        Assert.notNull(request, "request 不能为空");
        return AgentDTO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .systemPrompt(request.getSystemPrompt())
                .model(request.getModel() == null ? null : AgentDTO.ModelType.fromModelName(request.getModel()))
                .allowedTools(request.getAllowedTools())
                .allowedKbs(request.getAllowedKbs())
                .chatOptions(request.getChatOptions() == null
                        ? AgentDTO.ChatOptions.defaultOptions() : request.getChatOptions())
                .build();
    }

    /** PATCH 合并语义：null 不覆盖（api-contract.md §1） */
    public void updateDTOFromRequest(AgentDTO dto, UpdateAgentRequest request) {
        Assert.notNull(dto, "dto 不能为空");
        Assert.notNull(request, "request 不能为空");
        if (request.getName() != null) dto.setName(request.getName());
        if (request.getDescription() != null) dto.setDescription(request.getDescription());
        if (request.getSystemPrompt() != null) dto.setSystemPrompt(request.getSystemPrompt());
        if (request.getModel() != null) dto.setModel(AgentDTO.ModelType.fromModelName(request.getModel()));
        if (request.getAllowedTools() != null) dto.setAllowedTools(request.getAllowedTools());
        if (request.getAllowedKbs() != null) dto.setAllowedKbs(request.getAllowedKbs());
        if (request.getChatOptions() != null) dto.setChatOptions(request.getChatOptions());
    }

    public AgentVO toVO(AgentDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        return AgentVO.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .systemPrompt(dto.getSystemPrompt())
                .model(dto.getModel())
                .allowedTools(dto.getAllowedTools())
                .allowedKbs(dto.getAllowedKbs())
                .chatOptions(dto.getChatOptions())
                .build();
    }

    // ---- JSON 帮助方法（包内其他 Converter 复用同类写法） ----
    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 反序列化失败: " + json, e);
        }
    }

    private String toJsonList(List<String> list) {
        if (list == null) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank() || "null".equals(json)) return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 反序列化失败: " + json, e);
        }
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }
}
