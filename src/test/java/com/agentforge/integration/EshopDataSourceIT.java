package com.agentforge.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * eshop 第二数据源：只读连通 + 双重写保护（Hikari readOnly + DB 角色 eshop_ro 无写权限）。
 */
@Tag("integration")
@SpringBootTest
class EshopDataSourceIT {

    @Autowired
    private JdbcTemplate eshopJdbcTemplate;

    @Test
    void connectedToExpectedDatabaseAndUser() {
        String db = eshopJdbcTemplate.queryForObject("SELECT current_database()", String.class);
        String user = eshopJdbcTemplate.queryForObject("SELECT current_user", String.class);
        assertThat(db).as("eshop 数据源应连到 eshop_test 测试库").isEqualTo("eshop_test");
        assertThat(user).as("eshop 数据源应以只读角色连接").isEqualTo("eshop_ro");
    }

    @Test
    void readonlySelectWorks() {
        Integer products = eshopJdbcTemplate.queryForObject("SELECT count(*) FROM product", Integer.class);
        Integer categories = eshopJdbcTemplate.queryForObject("SELECT count(*) FROM category", Integer.class);
        assertThat(products).isEqualTo(10);
        assertThat(categories).isEqualTo(4);
    }

    @Test
    void writeIsRejectedAtBothLayers() throws Exception {
        // 第一层：Hikari readOnly 连接直接拒绝写（read-only transaction）
        assertThatThrownBy(() -> eshopJdbcTemplate.update("UPDATE product SET price = 1 WHERE id = 1"))
                .isInstanceOf(DataAccessException.class);

        // 第二层：绕过连接层只读，直连验证 DB 角色 eshop_ro 无写权限
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/eshop_test", "eshop_ro", "eshop_ro_local");
             java.sql.Statement st = conn.createStatement()) {
            assertThatThrownBy(() -> st.executeUpdate("UPDATE product SET price = 1 WHERE id = 1"))
                    .hasMessageContaining("permission denied");
        }
    }
}
