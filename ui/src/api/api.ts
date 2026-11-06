import { get, post, patch, del, upload } from "./http";
import type {
  AgentVO, ChatSessionVO, ChatMessageVO, KnowledgeBaseVO, DocumentVO, ToolVO,
  ModelType, ChatOptions,
} from "../types";

// ---- Agent（契约 §1：data 为 { agents: [...] } 等包裹结构）----
export interface AgentPayload {
  name: string;
  description?: string;
  systemPrompt?: string;
  model: ModelType;
  allowedTools?: string[];
  allowedKbs?: string[];
  chatOptions?: ChatOptions;
}

export const getAgents = () =>
  get<{ agents: AgentVO[] }>("/agents").then((r) => r.agents ?? []);

export const createAgent = (p: AgentPayload) =>
  post<{ agentId: string }>("/agents", p);

export const updateAgent = (id: string, p: Partial<AgentPayload>) =>
  patch<null>(`/agents/${id}`, p);

export const deleteAgent = (id: string) => del<null>(`/agents/${id}`);

// ---- 会话 ----
export const getChatSessions = () =>
  get<{ chatSessions: ChatSessionVO[] }>("/chat-sessions").then((r) => r.chatSessions ?? []);

export const createChatSession = (agentId: string, title?: string) =>
  post<{ chatSessionId: string }>("/chat-sessions", { agentId, title });

export const deleteChatSession = (id: string) => del<null>(`/chat-sessions/${id}`);

// ---- 消息 ----
export const getChatMessages = (sessionId: string) =>
  get<{ chatMessages: ChatMessageVO[] }>(`/chat-messages/session/${sessionId}`)
    .then((r) => r.chatMessages ?? []);

export const sendChatMessage = (agentId: string, sessionId: string, content: string) =>
  post<{ chatMessageId: string }>("/chat-messages", {
    agentId, sessionId, role: "user", content,
  });

// ---- 知识库 ----
export const getKnowledgeBases = () =>
  get<{ knowledgeBases: KnowledgeBaseVO[] }>("/knowledge-bases")
    .then((r) => r.knowledgeBases ?? []);

export const createKnowledgeBase = (name: string, description?: string) =>
  post<{ knowledgeBaseId: string }>("/knowledge-bases", { name, description });

export const deleteKnowledgeBase = (id: string) => del<null>(`/knowledge-bases/${id}`);

// ---- 文档 ----
export const getDocuments = (kbId: string) =>
  get<{ documents: DocumentVO[] }>(`/documents/kb/${kbId}`)
    .then((r) => r.documents ?? []);

export const uploadDocument = (kbId: string, file: File) => {
  const form = new FormData();
  form.append("kbId", kbId);
  form.append("file", file);
  return upload<{ documentId: string }>("/documents/upload", form);
};

export const deleteDocument = (id: string) => del<null>(`/documents/${id}`);

// ---- 工具（契约 §6：data 直接是数组）----
export const getTools = () =>
  get<ToolVO[]>("/tools").then((t) => t ?? []);
