# Endurance Admin 开发约定

本文件适用于 `endurance-admin/`。工作前读取本文件和[工程 README](README.md)；共同仓库约定见 [../AGENTS.md](../AGENTS.md)。

## 独立开发

- VS Code 直接打开本目录；依赖安装、启动、检查和构建都在本目录执行，不依赖 Web 的配置或依赖。
- Node.js 24、pnpm 11.25.0，具体版本以 `.node-version`、`package.json` 和 `pnpm-lock.yaml` 为准。
- 工具配置、锁文件和 `node_modules/` 属于本工程，不提升到仓库根目录，也不继承相邻工程的配置。
- 只修改当前任务授权的内容，不顺带改 Web、后端或仓库级配置。前端由用户维护，保留用户直接编辑与有意保留的格式。

## 技术与功能边界

React + TypeScript + Vite + Ant Design，TanStack Query 管理普通服务端数据，Zustand 管理本地状态。入口为 `src/main.tsx`，路由和 Provider 在 `src/app`，接口客户端在 `src/lib`。

通过 `/api` 与 Harness 交互，实时结果计划使用 SSE。管理页面路由不能代替后端权限校验，不将待建设入口当作已有接口契约。

当前概览读取真实 `/api/system/info`，会话、记忆和配置页面为明确标注的待建设入口，不提供模拟记录。后端没有默认管理员账号，用户和管理员认证尚未实现。新增功能时同步更新 README。

## 验证与运行

在本目录执行：

```bash
pnpm install --frozen-lockfile
pnpm check
pnpm format:check
pnpm build
```

`pnpm dev` 使用 `127.0.0.1:5174`，`pnpm preview` 使用 `127.0.0.1:5274`，均固定端口。涉及交互时验证对应页面、路由与流式状态；Vitest/Playwright 工具已预留，业务测试随实际功能增加。

本地代理配置写入 `.env.local`，示例见 `.env.example`；`VITE_` 变量会进入前端配置，不保存服务端密钥。`node_modules/`、`dist/`、测试产物、日志和本地配置不入库。不要为格式检查覆盖用户有意保留的内容。
