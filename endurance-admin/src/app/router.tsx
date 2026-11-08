import { createBrowserRouter } from 'react-router-dom';
import { AdminLayout } from '../layout/AdminLayout';
import { OverviewPage } from '../pages/OverviewPage';
import { PlaceholderPage } from '../pages/PlaceholderPage';
import { NotFoundPage } from '../pages/NotFoundPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AdminLayout />,
    children: [
      { index: true, element: <OverviewPage /> },
      {
        path: 'sessions',
        element: (
          <PlaceholderPage
            title="会话管理"
            description="会话、消息与运行记录将在接口实现后接入。"
          />
        ),
      },
      {
        path: 'memories',
        element: (
          <PlaceholderPage
            title="记忆管理"
            description="记忆写入、检索与维护将在能力实现后接入。"
          />
        ),
      },
      {
        path: 'settings',
        element: (
          <PlaceholderPage
            title="配置管理"
            description="模型、工具与沙箱的配置入口将在契约确定后接入。"
          />
        ),
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);
