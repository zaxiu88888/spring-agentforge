import { useCallback, useEffect, useRef, useState } from "react";
import { Typography, message } from "antd";
import { Sender } from "@ant-design/x";
import { getAgents, getChatSessions, getChatMessages, sendChatMessage } from "../api/api";
import { SSE_BASE_URL } from "../api/http";
import type { ChatMessageVO, SseMessage, SseMessageType } from "../types";
import MessageItem, { StatusBubble } from "./MessageItem";

interface Props {
  sessionId: string;
  onSessionDeleted: () => void;
}

/** 聊天主视图：历史加载 + SSE 实时推送 + 乐观发送（契约 §8 前端约定） */
export default function ChatView({ sessionId }: Props) {
  const [messages, setMessages] = useState<ChatMessageVO[]>([]);
  const [agentId, setAgentId] = useState<string>("");
  const [statusText, setStatusText] = useState<string | null>(null);
  const [statusType, setStatusType] = useState<"thinking" | "executing">("thinking");
  const [sending, setSending] = useState(false);
  const [inputValue, setInputValue] = useState("");

  const scrollRef = useRef<HTMLDivElement>(null);
  const nearBottomRef = useRef(true);

  const scrollToBottom = useCallback(() => {
    requestAnimationFrame(() => {
      const el = scrollRef.current;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }, []);

  // 会话信息（拿 agentId 供发消息用）+ 历史消息
  useEffect(() => {
    getChatSessions()
      .then((list) => {
        const s = list.find((x) => x.id === sessionId);
        if (s?.agentId) {
          setAgentId(s.agentId);
        } else {
          getAgents().then((agents) => agents[0] && setAgentId(agents[0].id)).catch(() => {});
        }
      })
      .catch(() => {});
    getChatMessages(sessionId).then((m) => { setMessages(m); scrollToBottom(); }).catch(() => {});
  }, [sessionId, scrollToBottom]);

  // SSE：连接本会话，四类事件驱动 UI（契约 §8）
  useEffect(() => {
    const es = new EventSource(`${SSE_BASE_URL}/connect/${sessionId}`);

    const handleMessage = (event: MessageEvent<string>) => {
      let sse: SseMessage;
      try {
        sse = JSON.parse(event.data) as SseMessage;
      } catch {
        return;
      }
      switch (sse.type as SseMessageType) {
        case "AI_GENERATED_CONTENT":
          if (sse.payload.message) {
            setMessages((prev) => [...prev, sse.payload.message!]);
          }
          break;
        case "AI_THINKING":
          setStatusType("thinking");
          setStatusText(sse.payload.statusText || "正在思考…");
          break;
        case "AI_EXECUTING":
          setStatusType("executing");
          setStatusText(sse.payload.statusText || "正在执行…");
          break;
        case "AI_DONE":
          setStatusText(null);
          if (sse.payload.statusText) {
            message.error(`Agent 异常：${sse.payload.statusText}`);
          }
          // 对账：拉一次全量，防 SSE 丢事件（契约 §8 前端约定）
          getChatMessages(sessionId)
            .then((fresh) => {
              setMessages((prev) => (fresh.length >= prev.length ? fresh : prev));
            })
            .catch(() => {});
          break;
      }
    };

    es.addEventListener("message", handleMessage);
    es.onerror = () => {
      // EventSource 会自动重连；重连成功后由 AI_DONE 之外的下一条消息触发对账
      // 保险起见：断线时也拉一次历史
      getChatMessages(sessionId).then((fresh) => setMessages(fresh)).catch(() => {});
    };
    return () => es.close();
  }, [sessionId]);

  // 新消息时若接近底部则跟随滚动
  useEffect(() => {
    if (nearBottomRef.current) scrollToBottom();
  }, [messages, statusText, scrollToBottom]);

  const onScroll = () => {
    const el = scrollRef.current;
    if (!el) return;
    nearBottomRef.current = el.scrollHeight - el.scrollTop - el.clientHeight <= 40;
  };

  const handleSend = async (content: string) => {
    if (!content.trim() || !agentId) return;
    setSending(true);
    setInputValue("");
    // 乐观插入用户消息
    setMessages((prev) => [
      ...prev,
      { id: `local-${Date.now()}`, sessionId, role: "user", content },
    ]);
    try {
      await sendChatMessage(agentId, sessionId, content);
      setStatusText("已发送，等待 Agent…");
    } catch {
      setStatusText(null);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="chat-view">
      <div
        ref={scrollRef}
        className="chat-scroll scrollbar-thin"
        onScroll={onScroll}
        style={{ background: "#fff" }}
      >
        {messages.length === 0 && (
          <Typography.Paragraph type="secondary" style={{ textAlign: "center", marginTop: 80 }}>
            向 Agent 提个问题吧——比如"哪个商品类目卖得最好？"或"退货政策是什么？"
          </Typography.Paragraph>
        )}
        {messages.map((m) => (
          <MessageItem key={m.id} message={m} />
        ))}
        {statusText && <StatusBubble text={statusText} type={statusType} />}
      </div>
      <div className="chat-input-bar">
        <Sender
          placeholder="输入消息，Enter 发送"
          disabled={sending}
          value={inputValue}
          onChange={setInputValue}
          onSubmit={handleSend}
        />
      </div>
    </div>
  );
}
