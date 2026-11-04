import { ConfigProvider, Layout, Typography } from "antd";
import zhCN from "antd/locale/zh_CN";

const { Content, Footer } = Layout;

/** M1 骨架占位页：三视图与聊天视图在 M8 实现（docs/mvp-scope.md） */
function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <Layout style={{ minHeight: "100vh" }}>
        <Content style={{ padding: 24 }}>
          <Typography.Title level={3}>AgentForge</Typography.Title>
          <Typography.Paragraph type="secondary">
            M1 工程骨架已就绪 —— Agent 管理 / 会话 / 知识库三视图与聊天主视图将在后续里程碑实现。
          </Typography.Paragraph>
        </Content>
        <Footer style={{ textAlign: "center" }}>
          AgentForge · Spring Boot 3.5 + Spring AI 1.1 · React 19 + Ant Design
        </Footer>
      </Layout>
    </ConfigProvider>
  );
}

export default App;
