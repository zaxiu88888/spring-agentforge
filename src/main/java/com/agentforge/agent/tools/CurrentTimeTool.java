package com.agentforge.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CurrentTimeTool implements AgentTool {

    public static final String NAME = "getCurrentTime";
    public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "获取当前系统时间（模型自身没有时间概念，回答时间相关问题必须先调用）";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool(name = NAME, description = "获取当前时间。format 为 Java 时间格式串，如 yyyy-MM-dd HH:mm:ss；留空使用默认格式")
    public String getCurrentTime(
            @ToolParam(description = "时间格式串，如 yyyy-MM-dd HH:mm:ss 或 yyyy年MM月dd日") String format) {
        DateTimeFormatter formatter;
        if (format == null || format.isBlank()) {
            formatter = DateTimeFormatter.ofPattern(DEFAULT_FORMAT);
        } else {
            try {
                formatter = DateTimeFormatter.ofPattern(format);
            } catch (IllegalArgumentException e) {
                formatter = DateTimeFormatter.ofPattern(DEFAULT_FORMAT);   // 非法格式回退默认，不报错打断 Agent
            }
        }
        return LocalDateTime.now().format(formatter);
    }
}
