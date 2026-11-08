import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import type { PropsWithChildren } from 'react';

const queryClient = new QueryClient({
  defaultOptions: { queries: { refetchOnWindowFocus: false } },
});

export function Providers({ children }: PropsWithChildren) {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#31685c',
          colorInfo: '#31685c',
          colorBgLayout: '#f4f6f3',
          borderRadius: 10,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", sans-serif',
        },
      }}
    >
      <AntApp>
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  );
}
