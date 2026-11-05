package com.agentforge.agent;

import com.agentforge.agent.tools.TerminateTool;
import com.agentforge.model.dto.ChatMessageDTO;
import com.agentforge.model.vo.ChatMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * Think-Execute 循环（tech-design.md §3.1，一次性实例不可重入）。
 *
 * 关键前提：chatOptions.internalToolExecutionEnabled=false —— 关闭框架的自动工具执行，
 * 工具是否执行、何时终止完全由本循环掌控，过程才可观测。
 *
 * 六条路径（与 AgentLoopTest 一一对应）：
 *  1 直接回答  2 terminate 终止  3 多轮工具  4 连续两轮工具回归  5 超步数  6 异常
 */
@Slf4j
public class AgentLoop {

    public static final int DEFAULT_MAX_STEPS = 20;

    private static final String THINK_PROMPT = """
            你现在是一个智能的「决策模块」。请根据当前对话上下文决定下一步动作：
            - 需要外部信息或操作时，调用合适的工具；
            - 已能回答或任务已完成时，直接给出最终回答（不再调用工具），或调用 terminate 主动终止。
            """;

    private final ChatClient chatClient;
    private final List<ToolCallback> toolCallbacks;
    private final ChatMemory chatMemory;
    private final String sessionId;
    private final int maxSteps;
    private final AgentMessageSink sink;
    private final AgentProgressListener progress;

    private final ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
    private final org.springframework.ai.model.tool.ToolCallingChatOptions chatOptions =
            DefaultToolCallingChatOptions.builder()
                    .internalToolExecutionEnabled(false)
                    .build();

    private AgentState state = AgentState.IDLE;
    private ChatResponse lastChatResponse;

    public AgentLoop(ChatClient chatClient,
                     List<ToolCallback> toolCallbacks,
                     ChatMemory chatMemory,
                     String sessionId,
                     int maxSteps,
                     AgentMessageSink sink,
                     AgentProgressListener progress) {
        this.chatClient = chatClient;
        this.toolCallbacks = List.copyOf(toolCallbacks);
        this.chatMemory = chatMemory;
        this.sessionId = sessionId;
        this.maxSteps = maxSteps;
        this.sink = sink;
        this.progress = progress;
    }

    public AgentState getState() {
        return state;
    }

    /** 装配到的工具回调名（日志与测试用） */
    public List<String> toolNames() {
        return toolCallbacks.stream().map(cb -> cb.getToolDefinition().name()).toList();
    }

    public void run() {
        if (state != AgentState.IDLE) {
            throw new IllegalStateException("Agent 循环不可重入，当前状态: " + state);
        }
        try {
            for (int step = 1; step <= maxSteps && state != AgentState.FINISHED; step++) {
                stepOnce();
                if (step == maxSteps && state != AgentState.FINISHED) {
                    log.warn("Agent 达到最大步数 {}，强制终止（路径5）", maxSteps);
                    state = AgentState.FINISHED;
                }
            }
            if (state == AgentState.IDLE || state == AgentState.THINKING || state == AgentState.EXECUTING) {
                state = AgentState.FINISHED;
            }
            progress.onDone(null);
        } catch (Exception e) {
            // 路径6：异常不向上抛（@Async 线程无人接），已产出的消息均已落库不丢失
            state = AgentState.ERROR;
            log.error("Agent 循环异常终止 session={}", sessionId, e);
            progress.onDone(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private void stepOnce() {
        if (think()) {
            execute();
        }
    }

    /** @return true = 模型要求调用工具（进入 execute）；false = 直接回答（循环结束，路径1） */
    private boolean think() {
        state = AgentState.THINKING;
        Prompt prompt = Prompt.builder()
                .messages(chatMemory.get(sessionId))
                .chatOptions(chatOptions)
                .build();

        lastChatResponse = chatClient.prompt(prompt)
                .system(THINK_PROMPT)
                .toolCallbacks(toolCallbacks.toArray(new ToolCallback[0]))
                .call()
                .chatClientResponse()
                .chatResponse();

        AssistantMessage output = lastChatResponse.getResult().getOutput();
        if (output.getToolCalls() == null || output.getToolCalls().isEmpty()) {
            ChatMessageVO vo = sink.persist(ChatMessageDTO.builder()
                    .sessionId(sessionId)
                    .role(ChatMessageDTO.RoleType.ASSISTANT)
                    .content(output.getText())
                    .build());
            progress.onMessage(vo);
            state = AgentState.FINISHED;      // 路径1：无工具调用即完成
            return false;
        }

        // 带工具调用的 assistant 消息：连同 toolCalls 元数据落库（记忆持久化协议）
        ChatMessageVO vo = sink.persist(ChatMessageDTO.builder()
                .sessionId(sessionId)
                .role(ChatMessageDTO.RoleType.ASSISTANT)
                .content(output.getText() == null ? "" : output.getText())
                .metadata(ChatMessageDTO.MetaData.builder()
                        .toolCalls(output.getToolCalls())
                        .build())
                .build());
        progress.onMessage(vo);
        progress.onThinking("决定调用: " + toolNames(output));
        return true;
    }

    private void execute() {
        state = AgentState.EXECUTING;
        progress.onExecuting(firstToolName());

        // 手动执行：回调必须随 options 传入，ToolCallingManager 才能按名解析并调用
        org.springframework.ai.model.tool.ToolCallingChatOptions execOptions =
                DefaultToolCallingChatOptions.builder()
                        .internalToolExecutionEnabled(false)
                        .toolCallbacks(toolCallbacks)
                        .build();
        List<Message> messagesBefore = chatMemory.get(sessionId);
        Prompt prompt = Prompt.builder()
                .messages(messagesBefore)
                .chatOptions(execOptions)
                .build();
        ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, lastChatResponse);

        // 记忆重建：先清空再整体回写，保证 assistant(tool_calls) 与 tool 响应成对相邻
        chatMemory.clear(sessionId);
        chatMemory.add(sessionId, result.conversationHistory());

        // 只持久化本轮新增部分（自执行前历史长度起）：
        // conversationHistory 含全部历史，整段遍历会把上一轮的工具响应重复落库
        boolean terminated = false;
        List<Message> rebuilt = result.conversationHistory();
        for (int i = messagesBefore.size(); i < rebuilt.size(); i++) {
            Message message = rebuilt.get(i);
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                    ChatMessageVO vo = sink.persist(ChatMessageDTO.builder()
                            .sessionId(sessionId)
                            .role(ChatMessageDTO.RoleType.TOOL)
                            .content(response.responseData())
                            .metadata(ChatMessageDTO.MetaData.builder()
                                    .toolResponse(response)
                                    .build())
                            .build());
                    progress.onMessage(vo);
                    if (TerminateTool.NAME.equals(response.name())) {
                        terminated = true;      // 路径2：terminate 信号
                    }
                }
            }
        }
        if (terminated) {
            state = AgentState.FINISHED;
        }
    }

    private String toolNames(AssistantMessage output) {
        return output.getToolCalls().stream()
                .map(AssistantMessage.ToolCall::name)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private String firstToolName() {
        if (lastChatResponse == null || !lastChatResponse.hasToolCalls()) {
            return "";
        }
        List<AssistantMessage.ToolCall> calls = lastChatResponse.getResult().getOutput().getToolCalls();
        return calls.isEmpty() ? "" : calls.get(0).name();
    }
}
