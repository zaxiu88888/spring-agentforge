// 与后端 api-contract.md §0 逐字对齐
export type ModelType = "glm-4.6" | "deepseek-chat";

export interface ChatOptions {
  temperature?: number;
  topP?: number;
  messageLength?: number;
}

export interface ToolVO {
  name: string;
  description: string;
  type: "FIXED" | "OPTIONAL";
}

export interface AgentVO {
  id: string;
  name: string;
  description?: string;
  systemPrompt?: string;
  model: ModelType;
  allowedTools?: string[];
  allowedKbs?: string[];
  chatOptions?: ChatOptions;
}

export interface ChatSessionVO {
  id: string;
  agentId: string | null;
  title?: string;
  updatedAt?: string;
}

export interface ToolCall {
  id: string;
  type: string;
  name: string;
  arguments: string; // JSON 字符串
}

export interface ToolResponse {
  id: string;
  name: string;
  responseData: string;
}

export type MessageRole = "user" | "assistant" | "system" | "tool";

export interface ChatMessageVO {
  id: string;
  sessionId: string;
  role: MessageRole;
  content: string;
  metadata?: {
    toolCalls?: ToolCall[];
    toolResponse?: ToolResponse;
  };
}

export interface KnowledgeBaseVO {
  id: string;
  name: string;
  description?: string;
}

export interface DocumentVO {
  id: string;
  kbId: string;
  filename: string;
  filetype?: string;
  size?: number;
}

export type SseMessageType =
  | "AI_THINKING"
  | "AI_EXECUTING"
  | "AI_GENERATED_CONTENT"
  | "AI_DONE";

export interface SseMessage {
  type: SseMessageType;
  payload: { message?: ChatMessageVO; statusText: string; done: boolean };
  metadata?: { chatMessageId?: string };
}
