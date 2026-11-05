package com.agentforge.agent;

import com.agentforge.model.dto.ChatMessageDTO;
import com.agentforge.model.vo.ChatMessageVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AgentLoop 六路径（mvp-scope.md M4 测试计划）。
 * 策略：ChatClient 深桩 mock 控制模型返回序列；ToolCallback 与 ToolCallingManager 全真——
 * 真正走通"声明→执行→记忆重建"的框架交互，比 mock 管理器更有回归价值。
 */
class AgentLoopTest {

    // ---- 测试用真实工具（@Tool 注解由 MethodToolCallbackProvider 扫描） ----
    static class StubTools {
        @Tool(name = "fakeQuery", description = "测试查询工具")
        public String fakeQuery(String keyword) {
            return "result-of-" + keyword;
        }

        @Tool(name = "terminate", description = "终止")
        public void terminate() {
        }
    }

    // ---- 收集器 ----
    static class RecordingSink implements AgentMessageSink {
        final List<ChatMessageDTO> persisted = new ArrayList<>();
        int seq = 0;

        @Override
        public ChatMessageVO persist(ChatMessageDTO dto) {
            persisted.add(dto);
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId("m" + (++seq));
            vo.setRole(dto.getRole().getValue());
            vo.setContent(dto.getContent());
            return vo;
        }
    }

    static class RecordingProgress implements AgentProgressListener {
        final List<String> events = new ArrayList<>();

        public void onThinking(String detail) { events.add("THINKING:" + detail); }

        public void onExecuting(String toolName) { events.add("EXECUTING:" + toolName); }

        public void onMessage(ChatMessageVO message) { events.add("MESSAGE:" + message.getRole()); }

        public void onDone(String errorSummary) { events.add("DONE:" + errorSummary); }
    }

    // ---- 构造辅助 ----

    private static ToolCallback[] callbacks() {
        return MethodToolCallbackProvider.builder().toolObjects(new StubTools()).build().getToolCallbacks();
    }

    private static ChatResponse response(String content, AssistantMessage.ToolCall... calls) {
        AssistantMessage message = AssistantMessage.builder()
                .content(content)
                .toolCalls(calls.length == 0 ? List.of() : List.of(calls))
                .build();
        return ChatResponse.builder().generations(List.of(new Generation(message))).build();
    }

    private static AssistantMessage.ToolCall call(String name, String args) {
        return new AssistantMessage.ToolCall("id-" + name, "function", name, args);
    }

    /** 深桩 mock ChatClient，按序返回给定响应 */
    private static ChatClient chatClientReturning(ChatResponse... responses) {
        ChatClient client = mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        var chain = when(client.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))
                .system(any(String.class))
                .toolCallbacks(any(ToolCallback[].class))
                .call()
                .chatClientResponse()
                .chatResponse());
        if (responses.length == 1) {
            chain.thenReturn(responses[0]);
        } else {
            chain.thenReturn(responses[0], java.util.Arrays.copyOfRange(responses, 1, responses.length));
        }
        return client;
    }

    private AgentLoop newLoop(ChatClient client, int maxSteps, RecordingSink sink, RecordingProgress progress) {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
        memory.add("s1", new org.springframework.ai.chat.messages.UserMessage("问题"));
        return new AgentLoop(client, List.of(callbacks()), memory, "s1", maxSteps, sink, progress);
    }

    // ---- 路径1：直接回答 ----
    @Test
    @DisplayName("路径1：无工具调用 → 持久化 assistant 消息后直接 FINISHED")
    void directAnswerFinishes() {
        RecordingSink sink = new RecordingSink();
        RecordingProgress progress = new RecordingProgress();
        ChatClient client = chatClientReturning(response("最终答案"));

        newLoop(client, 20, sink, progress).run();

        assertThat(sink.persisted).hasSize(1);
        assertThat(sink.persisted.get(0).getRole()).isEqualTo(ChatMessageDTO.RoleType.ASSISTANT);
        assertThat(sink.persisted.get(0).getContent()).isEqualTo("最终答案");
        assertThat(sink.persisted.get(0).getMetadata()).isNull();
        assertThat(progress.events).containsExactly("MESSAGE:assistant", "DONE:null");
    }

    // ---- 路径3：一轮工具 ----
    @Test
    @DisplayName("路径3：一轮工具调用 → 回答；toolCalls/toolResponse 元数据随消息落库，记忆重建")
    void oneToolRoundThenAnswer() {
        RecordingSink sink = new RecordingSink();
        RecordingProgress progress = new RecordingProgress();
        ChatClient client = chatClientReturning(
                response("", call("fakeQuery", "{\"keyword\":\"库存\"}")),
                response("查到了：result-of-库存"));

        AgentLoop loop = newLoop(client, 20, sink, progress);
        loop.run();

        assertThat(loop.getState()).isEqualTo(AgentState.FINISHED);
        assertThat(sink.persisted).extracting(d -> d.getRole().getValue())
                .containsExactly("assistant", "tool", "assistant");
        // 元数据持久化协议
        assertThat(sink.persisted.get(0).getMetadata().getToolCalls()).hasSize(1);
        assertThat(sink.persisted.get(1).getMetadata().getToolResponse().name()).isEqualTo("fakeQuery");
        // Spring AI 将工具返回值 JSON 序列化：字符串结果带引号
        assertThat(sink.persisted.get(1).getMetadata().getToolResponse().responseData())
                .isEqualTo("\"result-of-库存\"");
        // 过程事件序列
        assertThat(progress.events).containsExactly(
                "MESSAGE:assistant", "THINKING:决定调用: fakeQuery",
                "EXECUTING:fakeQuery", "MESSAGE:tool",
                "MESSAGE:assistant", "DONE:null");
    }

    // ---- 路径4：连续两轮工具（回归） ----
    @Test
    @DisplayName("路径4：连续两轮工具后再回答，消息序列完整")
    void twoToolRoundsThenAnswer() {
        RecordingSink sink = new RecordingSink();
        ChatClient client = chatClientReturning(
                response("", call("fakeQuery", "{\"keyword\":\"a\"}")),
                response("", call("fakeQuery", "{\"keyword\":\"b\"}")),
                response("两轮结果汇总"));

        AgentLoop loop = newLoop(client, 20, sink, new RecordingProgress());
        loop.run();

        assertThat(sink.persisted).extracting(d -> d.getRole().getValue())
                .containsExactly("assistant", "tool", "assistant", "tool", "assistant");
        assertThat(loop.getState()).isEqualTo(AgentState.FINISHED);
    }

    // ---- 路径2：terminate ----
    @Test
    @DisplayName("路径2：terminate 工具响应 → 立即 FINISHED，不再进入下一轮 think")
    void terminateStopsLoop() {
        RecordingSink sink = new RecordingSink();
        ChatClient client = chatClientReturning(
                response("", call("terminate", "{}")),
                response("不该出现"));   // 若未终止，第二次 think 会消费它

        AgentLoop loop = newLoop(client, 20, sink, new RecordingProgress());
        loop.run();

        assertThat(loop.getState()).isEqualTo(AgentState.FINISHED);
        assertThat(sink.persisted).extracting(d -> d.getRole().getValue())
                .containsExactly("assistant", "tool");
        assertThat(sink.persisted.get(1).getMetadata().getToolResponse().name()).isEqualTo("terminate");
    }

    // ---- 路径5：超步数 ----
    @Test
    @DisplayName("路径5：模型永远要求调工具 → 达到 maxSteps 强制 FINISHED")
    void maxStepsForcesFinish() {
        RecordingSink sink = new RecordingSink();
        ChatClient client = chatClientReturning(
                response("", call("fakeQuery", "{\"keyword\":\"1\"}")),
                response("", call("fakeQuery", "{\"keyword\":\"2\"}")),
                response("", call("fakeQuery", "{\"keyword\":\"3\"}")),
                response("", call("fakeQuery", "{\"keyword\":\"4\"}")));

        AgentLoop loop = newLoop(client, 2, sink, new RecordingProgress());
        loop.run();

        assertThat(loop.getState()).isEqualTo(AgentState.FINISHED);
        // 2 步 × (assistant+tool) = 4 条消息，无最终回答
        assertThat(sink.persisted).hasSize(4);
    }

    // ---- 路径6：异常 ----
    @Test
    @DisplayName("路径6：模型调用抛异常 → ERROR + onDone(错误摘要)，不向上抛")
    void exceptionSetsErrorWithoutRethrow() {
        RecordingProgress progress = new RecordingProgress();
        ChatClient client = mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(client.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))
                .system(any(String.class))
                .toolCallbacks(any(ToolCallback[].class))
                .call())
                .thenThrow(new RuntimeException("模型超时"));

        AgentLoop loop = newLoop(client, 20, new RecordingSink(), progress);

        assertThatCode(loop::run).doesNotThrowAnyException();
        assertThat(loop.getState()).isEqualTo(AgentState.ERROR);
        assertThat(progress.events).containsExactly("DONE:模型超时");
    }

    // ---- 记忆重建 ----
    @Test
    @DisplayName("execute 后记忆整体重建：assistant(tool_calls) 与 ToolResponseMessage 成对相邻")
    void memoryRebuiltAfterExecute() {
        RecordingSink sink = new RecordingSink();
        ChatClient client = chatClientReturning(
                response("", call("fakeQuery", "{\"keyword\":\"x\"}")),
                response("done"));

        MessageWindowChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
        memory.add("s1", new org.springframework.ai.chat.messages.UserMessage("问题"));
        AgentLoop loop = new AgentLoop(client, List.of(callbacks()), memory, "s1", 20, sink, new RecordingProgress());
        loop.run();

        List<Message> history = memory.get("s1");
        int assistantIdx = -1;
        int toolIdx = -1;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i) instanceof AssistantMessage am && am.getToolCalls() != null && !am.getToolCalls().isEmpty()) {
                assistantIdx = i;
            }
            if (history.get(i) instanceof ToolResponseMessage) {
                toolIdx = i;
            }
        }
        assertThat(assistantIdx).isGreaterThanOrEqualTo(0);
        assertThat(toolIdx).isEqualTo(assistantIdx + 1);   // 成对相邻
        // 设计决策：最终直接回答不回写内存记忆（实例一次性，答案已落库；下次运行由 Factory 从 DB 回放），
        // 因此循环结束后记忆最后一条是本轮的 ToolResponseMessage
        assertThat(history.get(history.size() - 1)).isInstanceOf(ToolResponseMessage.class);
    }
}
