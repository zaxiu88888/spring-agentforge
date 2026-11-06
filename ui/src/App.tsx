import { useState } from "react";
import { ConfigProvider, Typography } from "antd";
import zhCN from "antd/locale/zh_CN";
import Sidebar from "./components/Sidebar";
import ChatView from "./components/ChatView";
import KbView from "./components/KbView";

/** 主视图状态：欢迎页 / 聊天 / 知识库详情 */
export type MainView =
  | { kind: "welcome" }
  | { kind: "chat"; sessionId: string }
  | { kind: "kb"; kbId: string };

function App() {
  const [view, setView] = useState<MainView>({ kind: "welcome" });
  const [refreshSignal, setRefreshSignal] = useState(0);
  const bumpRefresh = () => setRefreshSignal((n) => n + 1);

  return (
    <ConfigProvider locale={zhCN}>
      <div className="app-shell">
        <div className="app-sidebar">
          <Sidebar
            view={view}
            onViewChange={setView}
            refreshSignal={refreshSignal}
          />
        </div>
        <div className="app-content">
          {view.kind === "welcome" && (
            <div style={{ padding: 48 }}>
              <Typography.Title level={3}>AgentForge</Typography.Title>
              <Typography.Paragraph type="secondary">
                从左侧选择或创建一个会话开始对话；在「智能体」页配置模型与工具；
                在「知识库」页上传 Markdown 文档。
              </Typography.Paragraph>
            </div>
          )}
          {view.kind === "chat" && (
            <ChatView key={view.sessionId} sessionId={view.sessionId} onSessionDeleted={bumpRefresh} />
          )}
          {view.kind === "kb" && <KbView kbId={view.kbId} onChanged={bumpRefresh} />}
        </div>
      </div>
    </ConfigProvider>
  );
}

export default App;
