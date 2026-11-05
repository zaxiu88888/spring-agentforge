package com.agentforge.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * eshop 演示库只读查询工具（tech-design.md §3.4）。
 *
 * 四层护栏（纵深防御，讲解稿 M5-1）：
 *  ① 语法白名单：仅 SELECT / WITH 开头；
 *  ② 多语句拒绝：出现分号即拒绝（含字符串内分号的误伤是可接受权衡）；
 *  ③ LIMIT 200 强制包裹：子查询外层封顶，无论模型写没写 LIMIT；
 *  ④ 数据源层只读：Hikari readOnly 连接 + eshop_ro 角色无写权限（M2 已验证）。
 * 返回结构化 JSON（M4 讲解稿第 9 题的落地：工具返回值直接设计成 JSON，不给框架"猜"的机会）。
 */
@Slf4j
@Component
public class SqlQueryTool implements AgentTool {

    public static final String NAME = "sqlQuery";
    public static final int MAX_ROWS = 200;

    /** @Tool 注解值必须是编译期常量，描述与 getDescription() 共用此常量保证一致 */
    private static final String DESCRIPTION =
            "查询电商演示数据库（类目/商品/客户/订单）。只读，仅支持单条 SELECT 语句，结果最多 200 行。"
                    + "常用表：category(id,name)、product(id,category_id,name,price,stock)、"
                    + "customer(id,name,city)、orders(id,customer_id,status)、order_item(order_id,product_id,quantity)";

    private final JdbcTemplate eshopJdbcTemplate;
    private final ObjectMapper objectMapper;

    public SqlQueryTool(JdbcTemplate eshopJdbcTemplate, ObjectMapper objectMapper) {
        this.eshopJdbcTemplate = eshopJdbcTemplate;   // 构造注入即 @Qualifier("eshopJdbcTemplate") 唯一匹配
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
        return ToolType.OPTIONAL;
    }

    @Tool(name = NAME, description = DESCRIPTION)
    public String sqlQuery(
            @ToolParam(description = "完整只读 SQL，如 SELECT c.name, SUM(oi.quantity) FROM order_item oi "
                    + "JOIN product p ON p.id = oi.product_id JOIN category c ON c.id = p.category_id "
                    + "GROUP BY c.name ORDER BY 2 DESC") String sql) {

        String violation = validate(sql);
        if (violation != null) {
            return error(violation);
        }
        String wrapped = wrapWithLimit(sql);
        try {
            List<Map<String, Object>> rows = eshopJdbcTemplate.queryForList(wrapped);
            return success(rows);
        } catch (DataAccessException e) {
            log.warn("[sqlQuery] 查询失败: {}", e.getMostSpecificCause().getMessage());
            return error("查询失败: " + e.getMostSpecificCause().getMessage());
        }
    }

    /** @return 违规描述；null 表示通过 */
    static String validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return "SQL 不能为空";
        }
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();   // 容忍单个收尾分号
        }
        String lower = trimmed.toLowerCase();
        if (!(lower.startsWith("select") || lower.startsWith("with"))) {
            return "仅支持 SELECT / WITH 查询，禁止任何写操作: " + shorten(trimmed);
        }
        if (trimmed.contains(";")) {
            return "不允许多条语句（检测到分号）";
        }
        return null;
    }

    /** 子查询包裹强制封顶：内层已有 LIMIT 也只会先取内层结果，外层 200 是硬上限 */
    static String wrapWithLimit(String sql) {
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return "SELECT * FROM (" + trimmed + ") AS _agentforge_q LIMIT " + MAX_ROWS;
    }

    private String success(List<Map<String, Object>> rows) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            if (!rows.isEmpty()) {
                result.put("columns", new ArrayList<>(rows.get(0).keySet()));
            } else {
                result.put("columns", List.of());
            }
            result.put("rowCount", rows.size());
            result.put("rows", rows);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return error("结果序列化失败");
        }
    }

    private String error(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message));
        } catch (Exception e) {
            return "{\"error\":\"" + message + "\"}";
        }
    }

    private static String shorten(String s) {
        return s.length() <= 60 ? s : s.substring(0, 60) + "...";
    }
}
