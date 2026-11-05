package com.agentforge.agent.tools;

/**
 * 工具管理接口：描述"这个工具是谁、什么类型"，用于清单管理与装配过滤。
 * 命名为 AgentTool 以避免与 Spring AI 的 @org.springframework.ai.tool.annotation.Tool 注解类型撞名
 * （后者本身也是接口，同名 import 会相互遮蔽）。
 * 真正暴露给 LLM 的签名由方法上的 @Tool 注解决定，
 * 由 Factory 经 MethodToolCallbackProvider 扫描成 ToolCallback（tech-design.md §2）。
 */
public interface AgentTool {

    /** 工具注册名：与 Agent.allowedTools 中的字符串、@Tool(name) 保持一致 */
    String getName();

    String getDescription();

    ToolType getType();
}
