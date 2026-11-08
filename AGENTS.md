# Endurance 仓库约定

本仓库承载三个独立工程，由一个 Git 仓库管理。根目录只保存工程导航、共同开发约定和 Git 配置，不作为 Maven 工程或 pnpm workspace，不在这里安装依赖或执行构建。

## 按工程读取开发约定

先确定本次任务涉及的工程，再读取该工程的 `AGENTS.md` 和 `README.md`。本文件提供共同约定；工程内的具体开发规则以该工程的 `AGENTS.md` 为准，更深目录如有 `AGENTS.md`，继续读取并遵循。

| 工程 | 用途 | 开发约定 | 工程说明 |
| --- | --- | --- | --- |
| `endurance-harness/` | Java 后端，IDEA 打开该目录的 `pom.xml` | [AGENTS.md](endurance-harness/AGENTS.md) | [README.md](endurance-harness/README.md) |
| `endurance-web/` | 聊天与运行入口，VS Code 单独打开该目录 | [AGENTS.md](endurance-web/AGENTS.md) | [README.md](endurance-web/README.md) |
| `endurance-admin/` | 管理后台，VS Code 单独打开该目录 | [AGENTS.md](endurance-admin/AGENTS.md) | [README.md](endurance-admin/README.md) |

跨工程任务分别读取涉及工程的约定。只修改当前任务授权的工程；前端由用户维护，没有明确的前端任务时，不顺带修改前端代码或配置。

## 工程边界

- 后端的 POM、Maven Wrapper、编辑器配置和验证命令归 `endurance-harness/`。
- 两个前端各自维护 `package.json`、`pnpm-lock.yaml`、Node/pnpm 版本声明、工具配置和 `node_modules/`，在各自目录安装、启动和构建。不要重新引入根目录前端工具链或跨工程配置继承。
- Web/Admin 共用 Java 后端。普通接口使用 HTTP，实时结果使用 SSE；Queue/Steer 属于运行时调度，具体契约随实际功能设计。
- 新增功能时核对实际代码，同步更新对应工程的 README 和验证结果。页面中的待建设入口与依赖声明不代表对应能力已实现。

## 修改与验证

保留用户直接编辑的内容和既有格式，特别是后端父 POM 的语义分块。检查和构建在对应工程目录执行，具体命令见工程的 `AGENTS.md`。

验证产物、日志、本地环境配置和截图不入库。运行或停止服务时核对命令与端口，避免影响其他项目进程。

## Git 与交付

按当前对话授权分别执行修改、提交和推送；提交不自动包含推送。暂存前检查改动来源与范围，不把本机配置、构建产物或其他任务改动一起纳入。交付说明实际改动、已运行的检查及尚未实现的能力。
