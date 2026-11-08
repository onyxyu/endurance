# Endurance 开发约定

工作前读取[仓库 README](README.md)和当前工程的 README，核对实际代码与当前任务授权。各工程可以单独打开和开发，Git、前端锁文件和公共工程配置统一位于仓库根目录。

## 工程入口

| 工程                 | 用途           | 说明                                                               |
| -------------------- | -------------- | ------------------------------------------------------------------ |
| `endurance-harness/` | Java 后端      | [README](endurance-harness/README.md)，IDEA 导入该目录的 `pom.xml` |
| `endurance-web/`     | 聊天与运行入口 | [README](endurance-web/README.md)                                  |
| `endurance-admin/`   | 管理后台       | [README](endurance-admin/README.md)                                |

前端由用户维护；没有明确的前端任务时，不顺带修改前端代码或共享前端配置。保留用户直接编辑，特别是根 POM 的语义分块格式。

## 已定架构

- 后端九模块为 common、api、core、models、tools、memory、sandbox、repository、server，均带 `endurance-` 前缀。
- common 保存领域无关基础能力；api 保存运行类型和 API/SPI；core 负责自研 Loop、上下文和生命周期；能力实现通过 api 协作，server 负责装配。
- api、common、core 保持与 Spring、模型 SDK、数据库和容器实现解耦。HTTP DTO 归 server，持久化实体和 Mapper 归 repository。
- 模型层采用 Spring AI 低层接口，Loop 自己实现；数据库使用 PostgreSQL，向量扩展方向为 pgvector，数据库访问采用 MyBatis-Plus。
- Web/Admin 共用 Java 后端。普通接口使用 HTTP，实时结果使用 SSE；Queue/Steer 属于运行时调度。具体消息和执行契约仍需按实际功能设计。
- POM 在主要配置块、内部模块、框架/迁移/测试等依赖组之间留空行，同类配置紧凑排列。

具体版本以父 POM、`package.json` 和 `pnpm-lock.yaml` 为准，不在新工程中另建互相冲突的版本来源。

## 当前基础骨架

默认 bootstrap profile 提供服务说明 HTML、`/api/system/info` 和 `/actuator/health`，无需模型 Key、PostgreSQL 或 Docker 服务。其他后端请求默认拒绝，没有默认管理员账号。

模型接入、Agent Loop、真实会话、Hook、记忆、数据库业务、沙箱执行和用户认证尚待实现。页面中的待建设入口与依赖声明不代表对应能力已完成。按新增功能同步更新 README 和验证结果。

## 验证

在后端目录运行 `./mvnw verify` 或 `mvn verify`；涉及普通 Java 改动时运行相应模块和必要的集成验证。仅调整 POM 空行时对比 XML 语义并运行 `mvn validate`。

前端在根目录统一安装锁文件依赖，执行 `pnpm check`、`pnpm build`；格式检查为 `pnpm format:check`。前端任务涉及交互时，再验证对应页面、路由与流式状态。不要为了格式检查覆盖用户有意保留的内容或后端 POM 分块。

验证产物、日志、本地环境配置和截图不入库。运行或停止服务时核对命令与端口，避免影响其他项目进程。

## Git 与交付

按当前对话授权分别执行修改、提交和推送；提交不自动包含推送。暂存前检查改动来源与范围，不把本机配置、构建产物或其他任务改动一起纳入。交付说明实际改动、已运行的检查及尚未实现的能力。
