package com.agentforge.rag;

import com.agentforge.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 智谱 embedding-3 客户端（tech-design.md §3.5）：dimensions=1024，与 chunk_embedding 表 VECTOR(1024) 对齐。
 * 3 次指数退避重试；哑 key 直接抛业务异常（启动告警在 MultiChatClientConfig，这里是使用点 fail-fast）。
 */
@Slf4j
@Component
public class ZhipuEmbeddingClient {

    public static final String MODEL = "embedding-3";
    public static final int DIMENSIONS = 1024;
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient restClient;
    private final String apiKey;

    public ZhipuEmbeddingClient(RestClient.Builder builder,
                                Environment environment,
                                @Value("${agentforge.embedding.base-url:https://open.bigmodel.cn}") String baseUrl) {
        this.apiKey = environment.getProperty("spring.ai.zhipuai.api-key", "");
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("dummy")) {
            throw new BizException("未配置 ZHIPU_API_KEY，无法执行向量化（上传文档/知识库检索都依赖它）");
        }
        RestClient.RequestBodySpec request = restClient.post()
                .uri("/api/paas/v4/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey);

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                EmbeddingResponse response = request
                        .body(new EmbeddingRequest(MODEL, text, DIMENSIONS))
                        .retrieve()
                        .body(EmbeddingResponse.class);
                if (response == null || response.data() == null || response.data().isEmpty()
                        || response.data().get(0).embedding() == null) {
                    throw new IllegalStateException("embedding 响应为空");
                }
                return response.data().get(0).embedding();
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("[embedding] 第 {} 次调用失败: {}", attempt, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleep(200L * (1L << (attempt - 1)));   // 200ms, 400ms
                }
            }
        }
        throw new BizException("向量化失败（已重试 " + MAX_ATTEMPTS + " 次）: "
                + (lastError == null ? "未知错误" : lastError.getMessage()));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    record EmbeddingRequest(String model, String input, int dimensions) {
    }

    record EmbeddingResponse(List<EmbeddingItem> data) {
        record EmbeddingItem(float[] embedding) {
        }
    }
}
