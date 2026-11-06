import { useCallback, useEffect, useState } from "react";
import { Button, Card, Empty, Popconfirm, Space, Table, Upload, Typography, message } from "antd";
import { UploadOutlined } from "@ant-design/icons";
import type { UploadProps } from "antd";
import { getKnowledgeBases, getDocuments, uploadDocument, deleteDocument } from "../api/api";
import type { DocumentVO } from "../types";

interface Props {
  kbId: string;
  onChanged: () => void;
}

function formatSize(bytes?: number) {
  if (bytes === undefined || bytes === null) return "—";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

/** 知识库详情：信息 + 上传 .md + 文档列表（契约 §5） */
export default function KbView({ kbId, onChanged }: Props) {
  const [kbName, setKbName] = useState("");
  const [docs, setDocs] = useState<DocumentVO[]>([]);
  const [uploading, setUploading] = useState(false);

  const refresh = useCallback(() => {
    getKnowledgeBases()
      .then((list) => setKbName(list.find((k) => k.id === kbId)?.name ?? ""))
      .catch(() => {});
    getDocuments(kbId).then(setDocs).catch(() => {});
  }, [kbId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const uploadProps: UploadProps = {
    accept: ".md",
    showUploadList: false,
    disabled: uploading,
    customRequest: async ({ file, onSuccess, onError }) => {
      setUploading(true);
      try {
        await uploadDocument(kbId, file as File);
        message.success(`${(file as File).name} 已上传并完成向量化`);
        onSuccess?.({});
        refresh();
        onChanged();
      } catch (e) {
        onError?.(e as Error);
      } finally {
        setUploading(false);
      }
    },
  };

  return (
    <div style={{ padding: 24, overflowY: "auto", height: "100%" }} className="scrollbar-thin">
      <Space direction="vertical" size="middle" style={{ width: "100%" }}>
        <Card size="small">
          <Typography.Title level={4} style={{ margin: 0 }}>
            知识库：{kbName || kbId}
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0, marginTop: 4 }}>
            上传 Markdown 文档后自动按标题分块并向量化，Agent 挂载本库即可用 knowledgeQuery 工具检索。
          </Typography.Paragraph>
        </Card>

        <Card size="small" title="上传文档（仅 .md）" extra={uploading ? "处理中…" : undefined}>
          <Upload {...uploadProps}>
            <Button icon={<UploadOutlined />}>选择 Markdown 文件</Button>
          </Upload>
        </Card>

        <Card size="small" title={`文档列表（${docs.length}）`}>
          {docs.length === 0 ? (
            <Empty description="暂无文档" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          ) : (
            <Table<DocumentVO>
              rowKey="id"
              size="small"
              pagination={{ pageSize: 10 }}
              dataSource={docs}
              columns={[
                { title: "文件名", dataIndex: "filename" },
                { title: "类型", dataIndex: "filetype", width: 70 },
                { title: "大小", dataIndex: "size", width: 90, render: formatSize },
                {
                  title: "操作",
                  width: 80,
                  render: (_, doc) => (
                    <Popconfirm
                      title="删除文档？"
                      description="向量分块将一并删除"
                      onConfirm={async () => {
                        await deleteDocument(doc.id);
                        message.success("已删除");
                        refresh();
                        onChanged();
                      }}
                    >
                      <Button size="small" type="link" danger>删除</Button>
                    </Popconfirm>
                  ),
                },
              ]}
            />
          )}
        </Card>
      </Space>
    </div>
  );
}
