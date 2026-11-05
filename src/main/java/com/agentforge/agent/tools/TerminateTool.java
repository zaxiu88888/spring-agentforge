package com.agentforge.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 终止信号工具：方法体刻意为空——它对模型只是一个"喊停"的出口，
 * 真正的终止逻辑由 AgentLoop 检查工具响应名实现（tech-design.md §3.1）。
 */
@Component
public class TerminateTool implements AgentTool {

    public static final String NAME = "terminate";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "任务已完成或无需继续时调用，主动终止 Agent 循环";
    }

    @Override
    public ToolType getType() {
        return ToolType.FIXED;
    }

    @Tool(name = NAME, description = "任务已完成或无需继续时调用此工具终止循环，无需参数")
    public void terminate() {
        // 信号工具：空实现，终止由主循环识别响应名完成
    }
}
