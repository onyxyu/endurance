# Endurance Web

个人自用的 Agent 聊天工作台。独立 React + TypeScript + Vite 工程，与 Java 后端通过 `/api` 交互。

VS Code 直接打开本目录。开发前读取本目录的 [AGENTS.md](AGENTS.md)。

## 开发

本地需要 Node.js 24、pnpm 11.25.0，可使用已有 pnpm 或 `corepack pnpm`，无需全局安装。工具依赖和版本由本目录的 `package.json`、`pnpm-lock.yaml` 管理，`node_modules/` 也在本目录生成。

在本目录执行：

```bash
pnpm install --frozen-lockfile
pnpm dev
```

默认地址为 `http://localhost:5173`；端口已占用时会直接报错，避免自动切换影响联调。安装、启动和构建均不依赖仓库根目录或 Admin 的前端配置。

后端需单独启动，默认代理目标为 `http://127.0.0.1:8080`。需要改地址时，将 `.env.example` 复制为 `.env.local` 后编辑。`VITE_` 变量属于前端配置，不保存服务端密钥。

## 检查与构建

```bash
pnpm check
pnpm format:check
pnpm build
pnpm preview
```

HTML 入口是 `index.html`，React 入口是 `src/main.tsx`，路由和 Provider 在 `src/app`，接口客户端在 `src/lib`。构建产物为 `dist/`，可交给静态服务器部署；生产环境需要将 `/api` 转发给 Java 后端，并将前端页面路由回退到 `index.html`。

`pnpm check` 执行 TypeScript 和 ESLint 检查，也可分别执行 `pnpm typecheck`、`pnpm lint`。`pnpm format:check` 只检查本工程；`pnpm preview` 默认地址为 `http://localhost:5273`。Vitest/Playwright 工具已预留，业务测试随实际功能增加，当前没有对应测试用例。

## 当前阶段

当前只有工程入口与基础页面，后端连接检查使用真实的 `/api/system/info`。聊天输入保留草稿，但发送与运行能力暂未接入。没有模拟模型回复、运行数据或 Agent Loop。

Ant Design X 提供对话界面组件，X Markdown 用于后续流式内容渲染；TanStack Query 管理普通服务端数据，Zustand 管理输入草稿等本地状态。Queue/Steer、工具和流式事件需要后续接入自有 Harness。
