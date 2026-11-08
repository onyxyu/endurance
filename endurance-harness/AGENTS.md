# Endurance Harness 开发约定

本文件适用于 `endurance-harness/`。工作前读取本文件、[工程 README](README.md)和涉及模块的 README；共同仓库约定见 [../AGENTS.md](../AGENTS.md)。

## 工程与架构

- IDEA 直接打开本目录的 `pom.xml`，本目录就是后端工程根目录，构建和运行不依赖两个前端工程。
- Java 21，版本以本目录父 POM 和 `.mvn/wrapper/maven-wrapper.properties` 为准，不另建冲突的版本来源。
- 九模块为 common、api、core、models、tools、memory、sandbox、repository、server，均带 `endurance-` 前缀。
- common 保存领域无关基础能力；api 保存运行类型和 API/SPI；core 负责自研 Loop、上下文和生命周期；能力实现通过 api 协作，server 负责装配。
- api、common、core 保持与 Spring、模型 SDK、数据库和容器实现解耦。HTTP DTO 归 server，持久化实体和 Mapper 归 repository。
- 模型层采用 Spring AI 低层接口，Loop 自己实现；数据库使用 PostgreSQL，向量扩展方向为 pgvector，数据库访问采用 MyBatis-Plus。
- Web/Admin 共用本后端。普通接口使用 HTTP，实时结果使用 SSE；Queue/Steer 属于运行时调度，具体消息和执行契约随实际功能设计。

## 修改范围与格式

只做当前任务授权的后端修改，不顺带修改前端。跨工程契约变更需核对受影响工程的开发约定与实际调用。

保留用户直接编辑的内容，特别是父 POM 的语义分块格式：主要配置块、内部模块、框架/迁移/测试等依赖组之间留空行，同类配置紧凑排列。不要用前端格式化工具批量处理本工程。

## 当前基础骨架

默认 bootstrap profile 提供服务说明 HTML、`/api/system/info` 和 `/actuator/health`，无需模型 Key、PostgreSQL 或 Docker 服务。其他后端请求默认拒绝，没有默认管理员账号。

模型接入、Agent Loop、真实会话、Hook、记忆、数据库业务、沙箱执行和用户认证尚待实现。依赖声明不代表能力已完成，按新增功能同步更新 README 和验证结果。

## 验证与运行

在本目录运行 `./mvnw verify` 或 `mvn verify`。涉及普通 Java 改动时运行相应模块和必要的集成验证；仅调整 POM 空行时，对比 XML 语义并运行 `mvn validate`。

启动方式和环境变量见 README，默认端口为 `8080`。运行或停止服务前核对命令、端口和进程，不影响其他工程。`target/`、日志、数据库凭据和本地配置不入库。
