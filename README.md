# Endurance

个人自用的云端 Agent 运行时。当前是可构建、可启动的工程骨架，Agent Loop、模型调用、数据库业务、工具与沙箱执行尚待实现。

本目录只是三个独立工程的父目录，统一由一个 Git 仓库管理。日常开发直接打开对应工程，安装、启动和构建均在工程目录执行。

| 工程 | 用途 | 打开方式 | 开发约定 |
| --- | --- | --- | --- |
| [endurance-harness](endurance-harness/README.md) | Java 后端，内部包含 9 个 Maven 模块 | IDEA 打开该目录的 `pom.xml` | [AGENTS.md](endurance-harness/AGENTS.md) |
| [endurance-web](endurance-web/README.md) | 普通聊天工作台 | VS Code 打开该目录 | [AGENTS.md](endurance-web/AGENTS.md) |
| [endurance-admin](endurance-admin/README.md) | 管理后台 | VS Code 打开该目录 | [AGENTS.md](endurance-admin/AGENTS.md) |

根目录只保留这份导航、[共同开发约定](AGENTS.md)和 Git 配置。前端各自维护工具配置、依赖、锁文件和 `node_modules/`；后端独立维护 Maven 工程。具体环境要求、启动命令、端口和当前能力见各工程 README。
