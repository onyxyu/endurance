# Endurance Web 开发约定

本文件适用于 `endurance-web/`。工作前读取本文件和[工程 README](README.md)；共同仓库约定见 [../AGENTS.md](../AGENTS.md)。

## 独立开发

- VS Code 直接打开本目录；依赖安装、启动、检查和构建都在本目录执行，不依赖 Admin 的配置或依赖。
- Node.js 24、pnpm 11.25.0，具体版本以 `.node-version`、`package.json` 和 `pnpm-lock.yaml` 为准。
- 工具配置、锁文件和 `node_modules/` 属于本工程，不提升到仓库根目录，也不继承相邻工程的配置。
- 只修改当前任务授权的内容，不顺带改 Admin、后端或仓库级配置。前端由用户维护，保留用户直接编辑与有意保留的格式。

## 技术与功能边界

React + TypeScript + Vite，Ant Design X 提供对话组件，X Markdown 用于后续流式内容渲染；TanStack Query 管理普通服务端数据，Zustand 管理输入草稿等本地状态。入口为 `src/main.tsx`，路由和 Provider 在 `src/app`，接口客户端在 `src/lib`。

通过 `/api` 与 Harness 交互，实时结果计划使用 SSE。Queue/Steer、工具和流式事件随后端契约接入，不凭空设计已完成的服务能力。

当前后端连接检查使用真实 `/api/system/info`；聊天输入保留草稿，发送与运行能力尚未接入，不提供模拟模型回复或运行数据。新增功能时同步更新 README。

## 验证与运行

在本目录执行：

```bash
pnpm install --frozen-lockfile
pnpm check
pnpm format:check
pnpm build
```

`pnpm dev` 使用 `127.0.0.1:5173`，`pnpm preview` 使用 `127.0.0.1:5273`，均固定端口。涉及交互时验证对应页面、路由与流式状态；Vitest/Playwright 工具已预留，业务测试随实际功能增加。

本地代理配置写入 `.env.local`，示例见 `.env.example`；`VITE_` 变量会进入前端配置，不保存服务端密钥。`node_modules/`、`dist/`、测试产物、日志和本地配置不入库。不要为格式检查覆盖用户有意保留的内容。
