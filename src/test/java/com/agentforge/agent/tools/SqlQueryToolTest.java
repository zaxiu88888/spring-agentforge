package com.agentforge.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** SQL 护栏纯单测（不碰数据库）：白名单/多语句/包裹规则 */
class SqlQueryToolTest {

    private final SqlQueryTool tool = new SqlQueryTool(Mockito.mock(JdbcTemplate.class), new ObjectMapper());

    @Test
    @DisplayName("validate：SELECT 与 WITH 放行")
    void allowsSelectAndWith() {
        assertThat(SqlQueryTool.validate("SELECT 1")).isNull();
        assertThat(SqlQueryTool.validate("  with t as (select 1) select * from t ")).isNull();
        assertThat(SqlQueryTool.validate("select name from product;")).isNull();   // 容忍收尾分号
    }

    @Test
    @DisplayName("validate：写操作与危险语句全拒")
    void rejectsWrites() {
        assertThat(SqlQueryTool.validate("UPDATE product SET price=1")).contains("仅支持 SELECT");
        assertThat(SqlQueryTool.validate("INSERT INTO product VALUES (1)")).contains("仅支持 SELECT");
        assertThat(SqlQueryTool.validate("DELETE FROM product")).contains("仅支持 SELECT");
        assertThat(SqlQueryTool.validate("DROP TABLE product")).contains("仅支持 SELECT");
        assertThat(SqlQueryTool.validate("TRUNCATE product")).contains("仅支持 SELECT");
        assertThat(SqlQueryTool.validate("")).isNotNull();
        assertThat(SqlQueryTool.validate(null)).isNotNull();
    }

    @Test
    @DisplayName("validate：分号多语句拒绝（含 SELECT;SELECT）")
    void rejectsMultiStatement() {
        assertThat(SqlQueryTool.validate("SELECT 1; SELECT 2")).contains("多条语句");
        assertThat(SqlQueryTool.validate("SELECT 1; DROP TABLE product")).contains("多条语句");
    }

    @Test
    @DisplayName("wrap：强制外层 LIMIT 200，容忍收尾分号")
    void wrapsWithLimit() {
        assertThat(SqlQueryTool.wrapWithLimit("SELECT name FROM product"))
                .isEqualTo("SELECT * FROM (SELECT name FROM product) AS _agentforge_q LIMIT 200");
        assertThat(SqlQueryTool.wrapWithLimit("SELECT name FROM product LIMIT 5;"))
                .isEqualTo("SELECT * FROM (SELECT name FROM product LIMIT 5) AS _agentforge_q LIMIT 200");
    }

    @Test
    @DisplayName("工具层拒绝时返回结构化 JSON 错误（而非抛异常打断 Agent）")
    void violationReturnsJsonError() {
        String result = tool.sqlQuery("DELETE FROM product");
        assertThat(result).contains("\"error\"").contains("仅支持 SELECT");
    }
}
