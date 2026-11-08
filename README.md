# Endurance

个人自用的云端 Agent 运行时。

当前是可构建、可启动的工程骨架，先明确模块与工程边界。Agent Loop、模型调用、数据库业务、工具与沙箱执行尚未实现。

## 三个独立工程

| 目录                                             | 用途                                | 入口说明                                 |
| ------------------------------------------------ | ----------------------------------- | ---------------------------------------- |
| [endurance-harness](endurance-harness/README.md) | Java 后端，内部包含 9 个 Maven 模块 | IDEA 打开该目录的 `pom.xml`              |
| [endurance-web](endurance-web/README.md)         | 普通聊天工作台                      | React + TypeScript + Vite + Ant Design X |
| [endurance-admin](endurance-admin/README.md)     | 管理后台                            | React + TypeScript + Vite + Ant Design   |

三个工程直接放在仓库根目录，统一由一个 Git 仓库管理。前端通过 pnpm workspace 共享工具版本，分别构建；Java 使用独立 Maven 父工程。仓库根目录不放聚合前后端的总 POM。

分项目继续开发前，先读取[仓库开发约定](AGENTS.md)和对应工程的 README。

## 本地环境

- Java 21。
- Maven 3.9.x，当前验证环境为 3.9.16。
- Node.js 24。
- pnpm 11.25.0，可通过本机已有 pnpm 或 `corepack pnpm` 使用，不需要在本仓库执行全局安装。

前端依赖由 `pnpm-lock.yaml` 锁定，父 POM 管理后端版本。TypeScript 使用 6.0.3，与当前 typescript-eslint 的支持范围匹配。

## 启动预览

先在仓库根目录构建后端：

```bash
mvn -f endurance-harness/pom.xml verify
```

终端一：

```bash
java -jar endurance-harness/endurance-server/target/endurance-server-0.1.0-SNAPSHOT.jar
```

终端二：

```bash
pnpm install --frozen-lockfile
pnpm dev:web
```

终端三：

```bash
pnpm dev:admin
```

| 地址                                  | 内容              |
| ------------------------------------- | ----------------- |
| http://localhost:8080                 | 后端服务说明 HTML |
| http://localhost:8080/api/system/info | 工程信息 JSON     |
| http://localhost:8080/actuator/health | 服务健康检查      |
| http://localhost:5173                 | Endurance Web     |
| http://localhost:5174                 | Endurance Admin   |

默认 bootstrap 模式可在没有模型 Key、PostgreSQL 或 Docker 服务时启动。前端代理目标默认 `http://127.0.0.1:8080`，可通过各前端工程 `.env.local` 的 `VITE_API_PROXY_TARGET` 调整。配置示例见各前端项目 `.env.example`；后端环境变量见 [Harness README](endurance-harness/README.md#外部服务配置)。

## 检查与构建

```bash
mvn -f endurance-harness/pom.xml verify
pnpm check
pnpm format:check
pnpm build
```

后端包含默认启动与只读入口/写接口边界的集成验证。前端目前执行 TypeScript、ESLint 与生产构建检查；Vitest/Playwright 工具已预留，业务测试随实际功能增加。

前端构建产物为各自 `dist/`，后端可执行 JAR 位于 `endurance-harness/endurance-server/target/`。`node_modules`、`dist`、`target`、本地配置与日志均已忽略，不进入 Git。

## 当前边界

- 后端只提供服务说明、健康检查与工程信息；其余请求默认拒绝，没有默认管理员账号。
- Web 的草稿可编辑，发送入口未开放，没有模拟模型回复。
- Admin 的概览读取真实工程信息；会话、记忆和配置是标注清楚的待建设页面。
- Spring AI、MyBatis-Plus/PostgreSQL、MCP、Docker 和 Flyway 的依赖已按模块组织，具体能力后续实现。
- 运行时消息、Queue/Steer、Hook、持久化与用户认证在后续设计中接入。TARS 不在当前仓库的初始化范围内。
