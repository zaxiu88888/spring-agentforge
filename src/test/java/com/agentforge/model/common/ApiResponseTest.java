package com.agentforge.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** ApiResponse 静态工厂与结构（api-contract.md §0） */
class ApiResponseTest {

    @Test
    @DisplayName("success(data) 携带数据与 success 描述")
    void successWithData() {
        ApiResponse<String> resp = ApiResponse.success("abc");
        assertThat(resp.getCode()).isEqualTo(200);
        assertThat(resp.getMessage()).isEqualTo("success");
        assertThat(resp.getData()).isEqualTo("abc");
    }

    @Test
    @DisplayName("success() 无数据时 data 为 null")
    void successWithoutData() {
        ApiResponse<Void> resp = ApiResponse.success();
        assertThat(resp.getCode()).isEqualTo(200);
        assertThat(resp.getData()).isNull();
    }

    @Test
    @DisplayName("error(message) 返回 code=500 且透传消息")
    void errorWithMessage() {
        ApiResponse<Void> resp = ApiResponse.error("Agent 不存在");
        assertThat(resp.getCode()).isEqualTo(500);
        assertThat(resp.getMessage()).isEqualTo("Agent 不存在");
        assertThat(resp.getData()).isNull();
    }
}
