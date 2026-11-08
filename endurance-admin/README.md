# Endurance Admin

Endurance Harness 管理端。独立 React + TypeScript + Vite 工程，与 Java 后端通过 `/api` 交互。

VS Code 直接打开本目录。开发前读取本目录的 [AGENTS.md](AGENTS.md)。

## 开发

本地需要 Node.js 24、pnpm 11.25.0，可使用已有 pnpm 或 `corepack pnpm`，无需全局安装。工具依赖和版本由本目录的 `package.json`、`pnpm-lock.yaml` 管理，`node_modules/` 也在本目录生成。

在本目录执行：

```bash
pnpm install --frozen-lockfile
pnpm dev
```

默认地址为 `http://localhost:5174`；端口已占用时会直接报错，避免自动切换影响联调。安装、启动和构建均不依赖仓库根目录或 Web 的前端配置。

后端需单独启动，默认代理目标为 `http://127.0.0.1:8080`。需要改地址时，将 `.env.example` 复制为 `.env.local` 后编辑。`VITE_` 变量属于前端配置，不保存服务端密钥。

## 检查与构建

```bash
pnpm check
pnpm format:check
pnpm build
pnpm preview
```

HTML 入口是 `index.html`，React 入口是 `src/main.tsx`，路由和 Provider 在 `src/app`，接口客户端在 `src/lib`。构建产物为 `dist/`，可交给静态服务器部署；生产环境需要将 `/api` 转发给 Java 后端，并将前端页面路由回退到 `index.html`。

`pnpm check` 执行 TypeScript 和 ESLint 检查，也可分别执行 `pnpm typecheck`、`pnpm lint`。`pnpm format:check` 只检查本工程；`pnpm preview` 默认地址为 `http://localhost:5274`。Vitest/Playwright 工具已预留，业务测试随实际功能增加，当前没有对应测试用例。

## 当前阶段

当前只有工程入口与基础页面，后端连接检查使用真实的 `/api/system/info`。会话、记忆和配置页面为明确标注的待建设入口，未放入模拟记录，也未实现管理员登录。

页面路由不会代替后端权限校验。当前 Java 服务只放开只读连接检查，其他接口默认拒绝；真实的用户和管理员认证后续设计。
