package com.agentforge.agent.tools;

import com.agentforge.model.dto.ChunkEmbeddingDTO;
import com.agentforge.service.RagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeQueryToolTest {

    private final RagService ragService = Mockito.mock(RagService.class);
    private final KnowledgeQueryTool tool = new KnowledgeQueryTool(ragService, new ObjectMapper());

    @Test
    @DisplayName("命中返回 title+content 的 JSON 数组")
    void returnsStructuredJson() {
        Mockito.when(ragService.similaritySearch("kb1", "退货", 3)).thenReturn(List.of(
                ChunkEmbeddingDTO.builder()
                        .content("7 天无理由退货")
                        .metadata(ChunkEmbeddingDTO.MetaData.builder().title("售后政策").build())
                        .build()));
        String result = tool.knowledgeQuery("kb1", "退货");
        assertThat(result).contains("\"title\":\"售后政策\"").contains("7 天无理由退货");
    }

    @Test
    @DisplayName("无命中返回空数组（模型自行判断无资料）")
    void emptyResults() {
        Mockito.when(ragService.similaritySearch(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(List.of());
        assertThat(tool.knowledgeQuery("kb1", "不存在的问题")).isEqualTo("[]");
    }

    @Test
    @DisplayName("检索异常返回 JSON 错误而非抛出")
    void errorAsJson() {
        Mockito.when(ragService.similaritySearch(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenThrow(new RuntimeException("向量服务超时"));
        assertThat(tool.knowledgeQuery("kb1", "x")).contains("\"error\"").contains("向量服务超时");
    }
}
