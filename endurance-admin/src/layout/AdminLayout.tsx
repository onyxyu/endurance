import { Menu } from 'antd';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ServiceStatus } from '../components/ServiceStatus';

const items = [
  { key: '/', label: '工程概览' },
  { key: '/sessions', label: '会话管理' },
  { key: '/memories', label: '记忆管理' },
  { key: '/settings', label: '配置管理' },
];

export function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  return (
    <div className="shell">
      <aside className="sidebar">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span>
          <div>
            <div className="brand-name">Endurance</div>
            <div className="brand-caption">Administration</div>
          </div>
        </Link>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={items}
          onClick={({ key }) => void navigate(key)}
        />
        <div className="sidebar-foot">
          Endurance Admin
          <br />
          认证与管理能力待接入
        </div>
      </aside>
      <main className="main">
        <header className="topbar">
          <h1>管理后台</h1>
          <ServiceStatus />
        </header>
        <div className="content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
