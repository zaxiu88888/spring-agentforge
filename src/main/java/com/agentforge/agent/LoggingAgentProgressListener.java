package com.agentforge.agent;

import com.agentforge.model.vo.ChatMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 默认实现：过程日志（M7 换成 SSE 推送实现，本实现保留作兜底/调试） */
@Slf4j
@Component
public class LoggingAgentProgressListener implements AgentProgressListener {

    @Override
    public void onThinking(String detail) {
        log.info("[agent] THINKING {}", detail);
    }

    @Override
    public void onExecuting(String toolName) {
        log.info("[agent] EXECUTING {}", toolName);
    }

    @Override
    public void onMessage(ChatMessageVO message) {
        log.info("[agent] MESSAGE role={} id={}", message.getRole(), message.getId());
    }

    @Override
    public void onDone(String errorSummary) {
        if (errorSummary == null) {
            log.info("[agent] DONE");
        } else {
            log.warn("[agent] DONE with error: {}", errorSummary);
        }
    }
}
