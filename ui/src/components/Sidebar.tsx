import { useCallback, useEffect, useState } from "react";
import { Button, Empty, List, Modal, Popconfirm, Select, Space, Tabs, Typography, message } from "antd";
import { DeleteOutlined, PlusOutlined, RobotOutlined } from "@ant-design/icons";
import AgentModal from "./AgentModal";
import type { MainView } from "../App";
import {
  getAgents, getChatSessions, getKnowledgeBases,
  createChatSession, deleteChatSession, deleteAgent, deleteKnowledgeBase, createKnowledgeBase,
} from "../api/api";
import type { AgentVO, ChatSessionVO, KnowledgeBaseVO } from "../types";

interface Props {
  view: MainView;
  onViewChange: (v: MainView) => void;
  refreshSignal: number;
}

/** 侧栏三 Tab：智能体 / 会话 / 知识库（会话点击进入聊天主视图） */
export default function Sidebar({ view, onViewChange, refreshSignal }: Props) {
  const [tab, setTab] = useState("chats");
  const [agents, setAgents] = useState<AgentVO[]>([]);
  const [sessions, setSessions] = useState<ChatSessionVO[]>([]);
  const [kbs, setKbs] = useState<KnowledgeBaseVO[]>([]);

  // 新建会话弹窗
  const [newChatOpen, setNewChatOpen] = useState(false);
  const [newChatAgent, setNewChatAgent] = useState<string>();
  // 新建知识库弹窗
  const [newKbOpen, setNewKbOpen] = useState(false);
  const [kbName, setKbName] = useState("");

  const [agentModalOpen, setAgentModalOpen] = useState(false);
  const [editingAgent, setEditingAgent] = useState<AgentVO | null>(null);

  const refreshAll = useCallback(() => {
    getAgents().then(setAgents).catch(() => {});
    getChatSessions().then(setSessions).catch(() => {});
    getKnowledgeBases().then(setKbs).catch(() => {});
  }, []);

  useEffect(() => {
    refreshAll();
  }, [refreshAll, refreshSignal]);

  const openNewChat = async () => {
    if (agents.length === 0) {
      message.info("请先在「智能体」页创建一个智能体");
      setTab("agents");
      return;
    }
    setNewChatAgent(agents[0].id);
    setNewChatOpen(true);
  };

  const createSession = async () => {
    if (!newChatAgent) return;
    const { chatSessionId } = await createChatSession(newChatAgent);
    setNewChatOpen(false);
    refreshAll();
    onViewChange({ kind: "chat", sessionId: chatSessionId });
  };

  const createKb = async () => {
    if (!kbName.trim()) return;
    await createKnowledgeBase(kbName.trim());
    setKbName("");
    setNewKbOpen(false);
    refreshAll();
  };

  const agentName = (id: string | null) =>
    agents.find((a) => a.id === id)?.name ?? "未绑定";

  return (
    <>
      <div style={{ padding: "14px 16px", fontWeight: 600, fontSize: 16, borderBottom: "1px solid #f0f0f0" }}>
        <RobotOutlined style={{ marginRight: 8, color: "#1677ff" }} />
        AgentForge
      </div>
      <Tabs
        activeKey={tab}
        onChange={setTab}
        style={{ padding: "0 12px", flex: 1, display: "flex", flexDirection: "column" }}
        items={[
          {
            key: "chats",
            label: "会话",
            children: (
              <>
                <Button block icon={<PlusOutlined />} onClick={openNewChat} style={{ marginBottom: 8 }}>
                  新会话
                </Button>
                <div className="list-scroll scrollbar-thin">
                  {sessions.length === 0 ? (
                    <Empty description="暂无会话" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                  ) : (
                    <List
                      dataSource={sessions}
                      renderItem={(s) => (
                        <List.Item
                          style={{
                            cursor: "pointer", padding: "8px 10px", borderRadius: 8,
                            background: view.kind === "chat" && view.sessionId === s.id ? "#e6f4ff" : undefined,
                          }}
                          onClick={() => onViewChange({ kind: "chat", sessionId: s.id })}
                          actions={[
                            <Popconfirm
                              key="del"
                              title="删除会话？"
                              description="聊天记录将一并删除"
                              onConfirm={async (e) => {
                                e?.stopPropagation();
                                await deleteChatSession(s.id);
                                if (view.kind === "chat" && view.sessionId === s.id) onViewChange({ kind: "welcome" });
                                refreshAll();
                              }}
                              onCancel={(e) => e?.stopPropagation()}
                            >
                              <Button size="small" type="text" danger icon={<DeleteOutlined />}
                                onClick={(e) => e.stopPropagation()} />
                            </Popconfirm>,
                          ]}
                        >
                          <List.Item.Meta
                            title={<span style={{ fontSize: 14 }}>{s.title || "新会话"}</span>}
                            description={<span style={{ fontSize: 12 }}>{agentName(s.agentId)}</span>}
                          />
                        </List.Item>
                      )}
                    />
                  )}
                </div>
              </>
            ),
          },
          {
            key: "agents",
            label: "智能体",
            children: (
              <>
                <Button block icon={<PlusOutlined />} style={{ marginBottom: 8 }}
                  onClick={() => { setEditingAgent(null); setAgentModalOpen(true); }}>
                  新建智能体
                </Button>
                <div className="list-scroll scrollbar-thin">
                  {agents.length === 0 ? (
                    <Empty description="暂无智能体" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                  ) : (
                    <List
                      dataSource={agents}
                      renderItem={(a) => (
                        <List.Item
                          style={{ cursor: "pointer", padding: "8px 10px", borderRadius: 8 }}
                          onClick={() => { setEditingAgent(a); setAgentModalOpen(true); }}
                          actions={[
                            <Popconfirm
                              key="del"
                              title="删除智能体？"
                              onConfirm={async (e) => {
                                e?.stopPropagation();
                                await deleteAgent(a.id);
                                refreshAll();
                              }}
                              onCancel={(e) => e?.stopPropagation()}
                            >
                              <Button size="small" type="text" danger icon={<DeleteOutlined />}
                                onClick={(e) => e.stopPropagation()} />
                            </Popconfirm>,
                          ]}
                        >
                          <List.Item.Meta
                            title={<span style={{ fontSize: 14 }}>{a.name}</span>}
                            description={
                              <span style={{ fontSize: 12 }}>
                                {a.model} · {(a.allowedTools ?? []).length} 个可选工具
                              </span>
                            }
                          />
                        </List.Item>
                      )}
                    />
                  )}
                </div>
              </>
            ),
          },
          {
            key: "kbs",
            label: "知识库",
            children: (
              <>
                <Button block icon={<PlusOutlined />} style={{ marginBottom: 8 }} onClick={() => setNewKbOpen(true)}>
                  新建知识库
                </Button>
                <div className="list-scroll scrollbar-thin">
                  {kbs.length === 0 ? (
                    <Empty description="暂无知识库" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                  ) : (
                    <List
                      dataSource={kbs}
                      renderItem={(kb) => (
                        <List.Item
                          style={{
                            cursor: "pointer", padding: "8px 10px", borderRadius: 8,
                            background: view.kind === "kb" && view.kbId === kb.id ? "#e6f4ff" : undefined,
                          }}
                          onClick={() => onViewChange({ kind: "kb", kbId: kb.id })}
                          actions={[
                            <Popconfirm
                              key="del"
                              title="删除知识库？"
                              description="文档与向量分块将一并删除"
                              onConfirm={async (e) => {
                                e?.stopPropagation();
                                await deleteKnowledgeBase(kb.id);
                                if (view.kind === "kb" && view.kbId === kb.id) onViewChange({ kind: "welcome" });
                                refreshAll();
                              }}
                              onCancel={(e) => e?.stopPropagation()}
                            >
                              <Button size="small" type="text" danger icon={<DeleteOutlined />}
                                onClick={(e) => e.stopPropagation()} />
                            </Popconfirm>,
                          ]}
                        >
                          <List.Item.Meta
                            title={<span style={{ fontSize: 14 }}>{kb.name}</span>}
                            description={<span style={{ fontSize: 12 }}>{kb.description || "—"}</span>}
                          />
                        </List.Item>
                      )}
                    />
                  )}
                </div>
              </>
            ),
          },
        ]}
      />

      {/* 新建会话 */}
      <Modal
        title="新会话"
        open={newChatOpen}
        onOk={createSession}
        onCancel={() => setNewChatOpen(false)}
        okText="创建并进入"
      >
        <Space direction="vertical" style={{ width: "100%" }} size="middle">
          <div>
            <Typography.Text type="secondary">选择智能体</Typography.Text>
            <Select
              style={{ width: "100%" }}
              value={newChatAgent}
              onChange={setNewChatAgent}
              options={agents.map((a) => ({ value: a.id, label: `${a.name}（${a.model}）` }))}
            />
          </div>
        </Space>
      </Modal>

      {/* 新建知识库 */}
      <Modal
        title="新建知识库"
        open={newKbOpen}
        onOk={createKb}
        onCancel={() => setNewKbOpen(false)}
        okText="创建"
      >
        <Typography.Text type="secondary">名称</Typography.Text>
        <input
          style={{ width: "100%", padding: "6px 10px", border: "1px solid #d9d9d9", borderRadius: 6, marginBottom: 12 }}
          value={kbName}
          onChange={(e) => setKbName(e.target.value)}
          placeholder="如：产品手册"
          onKeyDown={(e) => e.key === "Enter" && createKb()}
        />
      </Modal>

      {/* 智能体创建/编辑（单页表单；key 随开合/编辑对象变化 → 重挂载重置表单） */}
      <AgentModal
        key={agentModalOpen ? (editingAgent ? editingAgent.id : "new") : "closed"}
        open={agentModalOpen}
        editing={editingAgent}
        onClose={() => setAgentModalOpen(false)}
        onSaved={() => { setAgentModalOpen(false); refreshAll(); }}
      />
    </>
  );
}
