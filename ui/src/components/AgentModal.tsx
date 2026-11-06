import { useEffect, useState } from "react";
import { Checkbox, Input, Modal, Select, Slider, Space, Typography, message } from "antd";
import { createAgent, updateAgent, getTools, getKnowledgeBases, type AgentPayload } from "../api/api";
import type { AgentVO, KnowledgeBaseVO, ModelType, ToolVO } from "../types";

interface Props {
  open: boolean;
  editing: AgentVO | null;   // null = 新建
  onClose: () => void;
  onSaved: () => void;
}

const DEFAULT_OPTIONS = { temperature: 0.7, topP: 1.0, messageLength: 10 };

function initForm(editing: AgentVO | null): AgentPayload {
  if (!editing) {
    return {
      name: "", description: "", systemPrompt: "", model: "glm-4.6",
      allowedTools: [], allowedKbs: [], chatOptions: { ...DEFAULT_OPTIONS },
    };
  }
  return {
    name: editing.name,
    description: editing.description,
    systemPrompt: editing.systemPrompt,
    model: editing.model,
    allowedTools: editing.allowedTools ?? [],
    allowedKbs: editing.allowedKbs ?? [],
    chatOptions: editing.chatOptions ?? { ...DEFAULT_OPTIONS },
  };
}

/** 智能体配置单页表单（mvp-scope M8：不做多级菜单弹窗）。
 *  表单初值在挂载时由 useState 初始化器确定（父组件以 key 控制重挂载）。 */
export default function AgentModal({ open, editing, onClose, onSaved }: Props) {
  const [tools, setTools] = useState<ToolVO[]>([]);
  const [kbs, setKbs] = useState<KnowledgeBaseVO[]>([]);
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState<AgentPayload>(() => initForm(editing));

  useEffect(() => {
    if (open) {
      getTools().then(setTools).catch(() => {});
      getKnowledgeBases().then(setKbs).catch(() => {});
    }
  }, [open]);

  const set = <K extends keyof AgentPayload>(key: K, value: AgentPayload[K]) =>
    setForm((f) => ({ ...f, [key]: value }));

  const save = async () => {
    if (!form.name.trim()) {
      message.warning("请填写名称");
      return;
    }
    setSaving(true);
    try {
      if (editing) {
        await updateAgent(editing.id, form);
        message.success("已保存");
      } else {
        await createAgent(form);
        message.success("已创建");
      }
      onSaved();
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title={editing ? `编辑智能体：${editing.name}` : "新建智能体"}
      open={open}
      onCancel={onClose}
      onOk={save}
      okText={editing ? "保存" : "创建"}
      confirmLoading={saving}
      width={640}
      destroyOnHidden
    >
      <Space direction="vertical" style={{ width: "100%" }} size="middle">
        <div>
          <Typography.Text type="secondary">名称 *</Typography.Text>
          <Input value={form.name} onChange={(e) => set("name", e.target.value)} placeholder="如：数据助手" />
        </div>
        <div>
          <Typography.Text type="secondary">描述</Typography.Text>
          <Input value={form.description} onChange={(e) => set("description", e.target.value)} />
        </div>
        <div>
          <Typography.Text type="secondary">系统提示词</Typography.Text>
          <Input.TextArea
            rows={4}
            value={form.systemPrompt}
            onChange={(e) => set("systemPrompt", e.target.value)}
            placeholder="你是一个严谨的数据助手……"
          />
        </div>
        <div>
          <Typography.Text type="secondary">模型</Typography.Text>
          <Select
            style={{ width: "100%" }}
            value={form.model}
            onChange={(v: ModelType) => set("model", v)}
            options={[
              { value: "glm-4.6", label: "GLM-4.6（智谱）" },
              { value: "deepseek-chat", label: "DeepSeek Chat" },
            ]}
          />
        </div>
        <div>
          <Typography.Text type="secondary">
            温度 {form.chatOptions?.temperature?.toFixed(1)} / Top-P {form.chatOptions?.topP?.toFixed(1)} / 记忆窗口 {form.chatOptions?.messageLength} 条
          </Typography.Text>
          <Slider min={0} max={2} step={0.1} value={form.chatOptions?.temperature}
            onChange={(v) => set("chatOptions", { ...form.chatOptions!, temperature: v })} />
          <Slider min={0} max={1} step={0.1} value={form.chatOptions?.topP}
            onChange={(v) => set("chatOptions", { ...form.chatOptions!, topP: v })} />
          <Slider min={1} max={50} step={1} value={form.chatOptions?.messageLength}
            onChange={(v) => set("chatOptions", { ...form.chatOptions!, messageLength: v })} />
        </div>
        <div>
          <Typography.Text type="secondary">可选工具（terminate 与知识库检索已内置）</Typography.Text>
          <Checkbox.Group
            style={{ width: "100%" }}
            value={form.allowedTools}
            onChange={(v) => set("allowedTools", v as string[])}
            options={tools.map((t) => ({ value: t.name, label: `${t.name} — ${t.description.slice(0, 24)}…` }))}
          />
        </div>
        <div>
          <Typography.Text type="secondary">挂载知识库</Typography.Text>
          {kbs.length === 0 ? (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>（暂无知识库，可稍后在知识库页创建）</Typography.Text>
          ) : (
            <Checkbox.Group
              style={{ width: "100%" }}
              value={form.allowedKbs}
              onChange={(v) => set("allowedKbs", v as string[])}
              options={kbs.map((k) => ({ value: k.id, label: k.name }))}
            />
          )}
        </div>
      </Space>
    </Modal>
  );
}
