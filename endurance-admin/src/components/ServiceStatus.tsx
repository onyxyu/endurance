import { Badge, Button, Space } from 'antd';
import { useSystemInfo } from '../hooks/useSystemInfo';

export function ServiceStatus() {
  const query = useSystemInfo();
  const status = query.isPending ? 'processing' : query.isError ? 'default' : 'success';
  const text = query.isPending ? '正在检查后端' : query.isError ? '后端未连接' : '后端已连接';
  return (
    <Space wrap size="middle">
      <Badge status={status} text={text} />
      <Button size="small" onClick={() => void query.refetch()} loading={query.isFetching}>
        重新检查
      </Button>
    </Space>
  );
}
