package com.agentforge.agent;

/**
 * Agent 循环状态机（tech-design.md §3.1，真实流转非摆设）：
 * IDLE → THINKING →（有工具调用）EXECUTING → THINKING → … → FINISHED
 *                            └→（无工具调用）FINISHED
 * 任意异常 → ERROR（消息已落库不丢失）
 */
public enum AgentState {
    IDLE,
    THINKING,
    EXECUTING,
    FINISHED,
    ERROR
}
