package com.agentforge.rag;

import com.agentforge.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 哑 key fail-fast（使用点校验，对应启动告警的另一面） */
class ZhipuEmbeddingClientTest {

    private ZhipuEmbeddingClient clientWithKey(String key) {
        Environment env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty("spring.ai.zhipuai.api-key", "")).thenReturn(key);
        return new ZhipuEmbeddingClient(RestClient.builder(), env, "https://open.bigmodel.cn");
    }

    @Test
    @DisplayName("哑 key 调用直接抛业务异常并明示环境变量名")
    void dummyKeyFailsFast() {
        ZhipuEmbeddingClient client = clientWithKey("dummy-key-context-load");
        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ZHIPU_API_KEY");
    }

    @Test
    @DisplayName("空 key 同样 fail-fast")
    void emptyKeyFailsFast() {
        assertThatThrownBy(() -> clientWithKey("").embed("hello"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ZHIPU_API_KEY");
    }
}
