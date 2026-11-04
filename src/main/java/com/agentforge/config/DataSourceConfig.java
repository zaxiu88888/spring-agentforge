package com.agentforge.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 双数据源（tech-design.md §3.4）：
 * 主库 agenthub（@Primary，MyBatis 走它）+ 第二库 eshop（只读，SqlQueryTool 专用）。
 * 注意：容器中存在任何 DataSource Bean 都会使 Boot 自动装配退避，故主库也显式定义；
 * 多个同类型 Bean 一律 @Qualifier 按名注入，不依赖参数名推断。
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties agenthubDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("agenthubDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("eshop.datasource")
    public DataSourceProperties eshopDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public HikariDataSource eshopDataSource(
            @Qualifier("eshopDataSourceProperties") DataSourceProperties properties) {
        HikariDataSource ds = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        // 只读双保险：连接层只读 + 数据库角色 eshop_ro 无写权限
        ds.setReadOnly(true);
        ds.setPoolName("eshop-readonly");
        return ds;
    }

    @Bean
    public JdbcTemplate eshopJdbcTemplate(
            @Qualifier("eshopDataSource") DataSource eshopDataSource) {
        JdbcTemplate template = new JdbcTemplate(eshopDataSource);
        template.setQueryTimeout(10);
        return template;
    }
}
