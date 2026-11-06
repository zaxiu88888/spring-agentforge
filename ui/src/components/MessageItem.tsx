import { useState } from "react";
import { Tag, Typography } from "antd";
import { CheckCircleOutlined, ThunderboltOutlined } from "@ant-design/icons";
import { Bubble } from "@ant-design/x";
import XMarkdown from "@ant-design/x-markdown";
import type { ChatMessageVO } from "../types";

function parseArgsPreview(args: string): string {
  try {
    const obj = JSON.parse(args) as Record<string, unknown>;
    return Object.entries(obj)
      .slice(0, 2)
      .map(([k, v]) => `${k}=${String(v).slice(0, 40)}`)
      .join("  ");
  } catch {
    return args.slice(0, 60);
  }
}

function prettyJson(text: string): string {
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text;
  }
}

/** 工具结果卡片：工具名 + 可折叠结果 */
function ToolCard({ name, responseData }: { name: string; responseData: string }) {
  const [expanded, setExpanded] = useState(false);
  return (
    <div
      style={{
        margin: "4px 0 12px", padding: "8px 12px", borderRadius: 8,
        background: "#f6ffed", border: "1px solid #b7eb6f", cursor: "pointer",
        maxWidth: 680,
      }}
      onClick={() => setExpanded((v) => !v)}
    >
      <Typography.Text style={{ fontSize: 13 }}>
        <CheckCircleOutlined style={{ color: "#52c41a", marginRight: 6 }} />
        <Typography.Text code style={{ color: "#389e0d" }}>{name}</Typography.Text>
        <Typography.Text type="secondary" style={{ marginLeft: 8 }}>
          {responseData.slice(0, 90)}{responseData.length > 90 ? "…" : ""}
        </Typography.Text>
      </Typography.Text>
      {expanded && (
        <pre
          style={{
            marginTop: 8, maxHeight: 260, overflow: "auto", fontSize: 12,
            background: "#fff", padding: 8, borderRadius: 6, whiteSpace: "pre-wrap",
          }}
          className="scrollbar-thin"
        >
          {prettyJson(responseData)}
        </pre>
      )}
    </div>
  );
}

/** 单条消息渲染：user 右侧气泡 / assistant Markdown+工具调用标记 / tool 卡片 */
export default function MessageItem({ message }: { message: ChatMessageVO }) {
  if (message.role === "user") {
    return (
      <Bubble
        placement="end"
        content={message.content}
        styles={{ content: { maxWidth: 680, whiteSpace: "pre-wrap" } }}
      />
    );
  }

  if (message.role === "tool" && message.metadata?.toolResponse) {
    const { name, responseData } = message.metadata.toolResponse;
    return <ToolCard name={name} responseData={responseData} />;
  }

  if (message.role === "assistant") {
    const calls = message.metadata?.toolCalls ?? [];
    return (
      <div>
        {calls.map((c) => (
          <div
            key={c.id}
            style={{
              display: "inline-flex", alignItems: "center", gap: 6, margin: "4px 0 8px",
              padding: "3px 10px", borderRadius: 999, background: "#e6f4ff",
              border: "1px solid #91caff", fontSize: 12,
            }}
          >
            <ThunderboltOutlined style={{ color: "#1677ff" }} />
            <Typography.Text code style={{ color: "#0958d9" }}>{c.name}</Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {parseArgsPreview(c.arguments)}
            </Typography.Text>
          </div>
        ))}
        {message.content && (
          <div style={{ maxWidth: 720 }}>
            <XMarkdown content={message.content} />
          </div>
        )}
      </div>
    );
  }

  // system 等其他角色：居中灰字
  return (
    <Typography.Paragraph type="secondary" style={{ textAlign: "center", fontSize: 12 }}>
      {message.content}
    </Typography.Paragraph>
  );
}

/** Agent 过程状态气泡（THINKING / EXECUTING） */
export function StatusBubble({ text, type }: { text: string; type: "thinking" | "executing" }) {
  return (
    <div style={{ display: "flex", margin: "8px 0" }}>
      <div
        style={{
          padding: "6px 14px", borderRadius: 12, background: "#f0f5ff",
          border: "1px dashed #adc6ff", fontSize: 13, color: "#1d39c4",
          animation: "ant-pulse 1.5s ease-in-out infinite",
        }}
      >
        {type === "thinking" ? "✨ " : "⚡ "}
        {text}
        <Tag style={{ marginLeft: 8 }} color={type === "thinking" ? "processing" : "gold"}>
          {type === "thinking" ? "思考中" : "执行中"}
        </Tag>
      </div>
    </div>
  );
}
