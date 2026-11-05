package com.agentforge.integration;

import com.agentforge.agent.AgentLoop;
import com.agentforge.agent.AgentRuntimeFactory;
import com.agentforge.mapper.AgentMapper;
import com.agentforge.model.entity.Agent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实工具 Bean 的装配走查：勾选 sqlQuery + getCurrentTime 的 Agent，
 * 装配结果必须含 FIXED 的 terminate 与两个 OPTIONAL（MethodToolCallbackProvider 扫描真实 @Tool Bean）。
 */
@Tag("integration")
@SpringBootTest
class AgentToolAssemblyIT {

    @Autowired private AgentRuntimeFactory factory;
    @Autowired private AgentMapper agentMapper;

    @Test
    void assemblyIncludesFixedAndAllowedOptionalTools() {
        Agent agent = Agent.builder()
                .name("tool-assembly-it")
                .model("glm-4.6")
                .systemPrompt("测试")
                .allowedTools("[\"sqlQuery\",\"getCurrentTime\"]")
                .build();
        agentMapper.insert(agent);
        try {
            AgentLoop loop = factory.create(agent.getId(), java.util.UUID.randomUUID().toString());
            assertThat(loop.toolNames())
                    .containsExactlyInAnyOrder("terminate", "sqlQuery", "getCurrentTime");
        } finally {
            agentMapper.deleteById(agent.getId());
        }
    }
}
