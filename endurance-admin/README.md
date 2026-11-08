# Endurance Admin

Endurance Harness 管理端。独立 React + TypeScript + Vite 工程，与 Java 后端通过 `/api` 交互。

## 开发

在仓库根目录执行：

```bash
pnpm install --frozen-lockfile
pnpm dev:admin
```

也可在本目录执行 `pnpm dev`。默认地址为 `http://localhost:5174`；端口已占用时会直接报错，避免自动切换影响联调。

后端需单独启动，默认代理目标为 `http://127.0.0.1:8080`。需要改地址时，将 `.env.example` 复制为 `.env.local` 后编辑。`VITE_` 变量属于前端配置，不保存服务端密钥。

## 检查与构建

```bash
pnpm typecheck
pnpm build
pnpm preview
```

HTML 入口是 `index.html`，React 入口是 `src/main.tsx`，路由和 Provider 在 `src/app`，接口客户端在 `src/lib`。构建产物为 `dist/`，可交给静态服务器部署；生产环境需要将 `/api` 转发给 Java 后端，并将前端页面路由回退到 `index.html`。

## 当前阶段

当前只有工程入口与基础页面，后端连接检查使用真实的 `/api/system/info`。会话、记忆和配置页面为明确标注的待建设入口，未放入模拟记录，也未实现管理员登录。

页面路由不会代替后端权限校验。当前 Java 服务只放开只读连接检查，其他接口默认拒绝；真实的用户和管理员认证后续设计。
