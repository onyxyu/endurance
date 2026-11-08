import { Card, Empty, Tag, Typography } from 'antd';

export function PlaceholderPage({ title, description }: { title: string; description: string }) {
  return (
    <div className="stack">
      <div>
        <Typography.Title level={2}>{title}</Typography.Title>
        <Tag>待建设</Tag>
      </div>
      <Card>
        <Empty description={description} />
      </Card>
    </div>
  );
}
