package com.agentforge.agent.tools;

import com.agentforge.model.dto.ChunkEmbeddingDTO;
import com.agentforge.service.RagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库检索工具（FIXED：所有 Agent 必装，think 提示词注入的可用知识库列表配合使用）。
 * 返回结构化 JSON（title + content），模型据此引用或继续追问。
 */
@Component
public class KnowledgeQueryTool implements AgentTool {

    public static final String NAME = "knowledgeQuery";
    private static final int TOP_K = 3;

    private static final String DESCRIPTION =
            "在指定知识库中做语义检索，返回最相关的文档片段。需要私有知识（产品手册/规范/资料）时必须先调用本工具，"
                    + "kbId 从系统提示的【可用知识库】列表中选择";

    private final RagService ragService;
    private final ObjectMapper objectMapper;

    public KnowledgeQueryTool(RagService ragService, ObjectMapper objectMapper) {
        this.ragService = ragService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public ToolType getType() {
        return ToolType.FIXED;
    }

    @Tool(name = NAME, description = DESCRIPTION)
    public String knowledgeQuery(
            @ToolParam(description = "知识库 ID，从【可用知识库】列表选择") String kbId,
            @ToolParam(description = "检索问题，如：退货政策是什么") String query) {
        try {
            List<ChunkEmbeddingDTO> hits = ragService.similaritySearch(kbId, query, TOP_K);
            String json = objectMapper.writeValueAsString(hits.stream()
                    .map(hit -> new Hit(
                            hit.getMetadata() == null ? null : hit.getMetadata().getTitle(),
                            hit.getContent()))
                    .toList());
            return json.isEmpty() ? "{\"results\":[]}" : json;
        } catch (Exception e) {
            return error("知识库检索失败: " + e.getMessage());
        }
    }

    private record Hit(String title, String content) {
    }

    private String error(String message) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of("error", message));
        } catch (Exception e) {
            return "{\"error\":\"知识库检索失败\"}";
        }
    }
}
