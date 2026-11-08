import { Alert, Card, Descriptions, Skeleton, Tag, Typography } from 'antd';
import { useSystemInfo } from '../hooks/useSystemInfo';

export function OverviewPage() {
  const query = useSystemInfo();
  const info = query.data;
  return (
    <div className="stack">
      <div>
        <div className="page-label">Endurance / Admin</div>
        <Typography.Title className="hero-title">从清楚的边界开始。</Typography.Title>
        <Typography.Paragraph type="secondary">
          当前仅展示真实服务信息与工程结构。
        </Typography.Paragraph>
      </div>
      <div className="grid">
        <Card title="后端连接" className="full">
          {query.isPending ? (
            <Skeleton active paragraph={{ rows: 2 }} />
          ) : info ? (
            <Descriptions
              column={{ xs: 1, sm: 3 }}
              items={[
                { key: 'name', label: '服务', children: info.name },
                { key: 'version', label: '版本', children: info.version },
                { key: 'stage', label: '阶段', children: <Tag>{info.stage}</Tag> },
              ]}
            />
          ) : (
            <Alert
              type="info"
              showIcon
              title="尚未连接后端"
              description="启动 endurance-harness 后，点击右上角重新检查。"
            />
          )}
        </Card>
        <Card title="Maven 模块" className="full">
          {info ? (
            <div className="module-tags">
              {info.modules.map((module) => (
                <Tag key={module}>endurance-{module}</Tag>
              ))}
            </div>
          ) : (
            <Typography.Text type="secondary">连接后从后端读取模块信息。</Typography.Text>
          )}
        </Card>
        <Card title="会话与运行">
          <Typography.Paragraph type="secondary">
            当前没有接入会话、运行和模型服务，暂不展示统计或模拟记录。
          </Typography.Paragraph>
          <Tag>待建设</Tag>
        </Card>
        <Card title="访问与配置">
          <Typography.Paragraph type="secondary">
            管理员身份、权限与配置操作将在接口设计后实现。
          </Typography.Paragraph>
          <Tag>待建设</Tag>
        </Card>
      </div>
      <div className="bootstrap-note">此页是管理前端的基础入口，业务管理操作尚未开放。</div>
    </div>
  );
}
