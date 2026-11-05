package com.agentforge.agent.tools;

/**
 * 工具分类：FIXED 全 Agent 必装（terminate、知识库检索）；
 * OPTIONAL 按 Agent.allowedTools 配置勾选（SQL、时间等）。
 */
public enum ToolType {
    FIXED,
    OPTIONAL
}
