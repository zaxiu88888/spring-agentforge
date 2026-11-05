package com.agentforge.integration;

import com.agentforge.agent.tools.SqlQueryTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** 真库走查：白名单内的查询命中 eshop_test 种子数据；拒绝路径返回 JSON 错误 */
@Tag("integration")
@SpringBootTest
class SqlQueryToolIT {

    @Autowired private SqlQueryTool tool;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void queryReturnsStructuredJson() throws Exception {
        String result = tool.sqlQuery(
                "SELECT p.name, p.price FROM product p JOIN category c ON c.id = p.category_id "
                        + "WHERE c.name = '手机数码' ORDER BY p.price DESC");

        JsonNode json = objectMapper.readTree(result);
        assertThat(json.has("error")).isFalse();
        assertThat(json.path("columns").toString()).contains("name").contains("price");
        assertThat(json.path("rowCount").asInt()).isEqualTo(3);   // 手机数码 3 件商品
        assertThat(json.path("rows").get(0).path("name").asText()).isEqualTo("旗舰手机 Pro 256G");
    }

    @Test
    void aggregationQueryWorks() throws Exception {
        // 验收场景的原型：哪类商品卖得最好（数量口径）
        String result = tool.sqlQuery(
                "SELECT c.name AS category, SUM(oi.quantity) AS sold FROM order_item oi "
                        + "JOIN product p ON p.id = oi.product_id "
                        + "JOIN category c ON c.id = p.category_id "
                        + "GROUP BY c.name ORDER BY sold DESC");

        JsonNode json = objectMapper.readTree(result);
        assertThat(json.path("rows").get(0).path("category").asText()).isEqualTo("手机数码");
    }

    @Test
    void emptyResultStillValidJson() throws Exception {
        String result = tool.sqlQuery("SELECT id FROM product WHERE id = 99999");
        JsonNode json = objectMapper.readTree(result);
        assertThat(json.path("rowCount").asInt()).isZero();
        assertThat(json.path("rows").isEmpty()).isTrue();
    }

    @Test
    void badSqlReturnsErrorJson() throws Exception {
        String result = tool.sqlQuery("SELECT no_such_column FROM product");
        assertThat(objectMapper.readTree(result).path("error").asText()).contains("查询失败");
    }
}
