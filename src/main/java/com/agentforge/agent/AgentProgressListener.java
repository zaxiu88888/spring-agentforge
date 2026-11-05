package com.agentforge.agent;

import com.agentforge.model.vo.ChatMessageVO;

/**
 * Agent 过程回调（M4 定义，M7 的 SSE 实现同构映射）：
 * onThinking → AI_THINKING；onExecuting → AI_EXECUTING；
 * onMessage → AI_GENERATED_CONTENT；onDone → AI_DONE（errorSummary 非空表示异常终止）。
 * 实现必须线程安全且不抛异常（回调失败不能影响循环本身）。
 */
public interface AgentProgressListener {

    void onThinking(String detail);

    void onExecuting(String toolName);

    void onMessage(ChatMessageVO message);

    void onDone(String errorSummary);
}
