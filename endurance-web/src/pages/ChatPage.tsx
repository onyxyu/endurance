import { Sender, Welcome } from '@ant-design/x';
import { Button, Card, Tag, Typography } from 'antd';
import { Link } from 'react-router-dom';
import { ServiceStatus } from '../components/ServiceStatus';
import { useConversationStore } from '../features/conversation/store';

export function ChatPage() {
  const draft = useConversationStore((state) => state.draft);
  const setDraft = useConversationStore((state) => state.setDraft);

  return (
    <div className="shell">
      <aside className="sidebar">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span>
          <div>
            <div className="brand-name">Endurance</div>
            <div className="brand-caption">Personal Agent Workspace</div>
          </div>
        </Link>
        <Button block disabled>
          新建会话 · 待接入
        </Button>
        <div style={{ padding: '0 12px' }}>
          <Typography.Text type="secondary">会话</Typography.Text>
          <p className="muted">尚无会话记录</p>
        </div>
        <div className="sidebar-foot">
          Endurance Web
          <br />
          个人自用的云端 Agent 入口
        </div>
      </aside>
      <main className="main">
        <header className="topbar">
          <h1>工作台</h1>
          <ServiceStatus />
        </header>
        <div className="content stack">
          <div>
            <div className="page-label">Endurance / Web</div>
            <Typography.Title className="hero-title">给想法一个执行入口。</Typography.Title>
            <Typography.Paragraph type="secondary">
              聊天、工具与运行过程将在这里汇合。
            </Typography.Paragraph>
          </div>
          <Card>
            <div className="empty-chat">
              <Welcome
                icon={<span className="brand-mark">E</span>}
                title="欢迎来到 Endurance"
                description="基础页面已初始化。会话接口和模型能力接入后，即可从这里开始对话。"
              />
            </div>
          </Card>
          <section className="composer" aria-label="消息输入">
            <Sender
              value={draft}
              onChange={setDraft}
              autoSize={{ minRows: 2, maxRows: 6 }}
              placeholder="可以先写下想法，草稿仅保留在当前页面。"
              suffix={
                <Button type="primary" disabled>
                  发送 · 待接入
                </Button>
              }
            />
            <div className="composer-note">当前为工程骨架，尚未发送模型请求。</div>
          </section>
          <div className="status-row">
            <Tag>基础骨架</Tag>
            <Typography.Text type="secondary">
              Loop、工具调用和运行中追加消息将逐步接入。
            </Typography.Text>
          </div>
        </div>
      </main>
    </div>
  );
}
