# Endurance Harness

个人自用的云端 Agent 运行时后端。Java 21、Spring Boot 4.1.2；当前为可启动的多模块工程骨架。

本目录是独立后端工程，IDEA 直接打开这里的 `pom.xml`。开发前读取本目录的 [AGENTS.md](AGENTS.md)，无需从仓库根目录启动或构建。

## 工程结构

| Maven 模块 | 职责 |
| --- | --- |
| endurance-common | 领域无关的公共基础 |
| endurance-api | 运行、消息和能力扩展契约 |
| endurance-core | 自研 Loop、上下文与生命周期 |
| endurance-models | Spring AI 模型接入 |
| endurance-tools | 内置工具与 MCP 接入 |
| endurance-memory | 记忆策略 |
| endurance-sandbox | Docker 等执行环境适配 |
| endurance-repository | MyBatis-Plus / PostgreSQL 数据库访问 |
| endurance-server | 服务入口、接口、认证及组件装配 |

Core 和能力实现通过 API 协作；Server 负责装配。所有模块在一个 Maven 工程内构建，IDEA 打开本目录的 `pom.xml` 即可。

## 构建与启动

在本目录执行：

```bash
mvn verify
mvn install -DskipTests
mvn -pl endurance-server spring-boot:run
```

也提供可选的 Maven Wrapper：macOS/Linux 可用 `./mvnw verify`，Windows 可用 `mvnw.cmd verify`。Wrapper 固定 Maven 3.9.16，首次使用会下载该版本；已有本机 Maven 时可继续直接使用 `mvn`。

也可以在 `mvn package` 后直接运行：

```bash
java -jar endurance-server/target/endurance-server-0.1.0-SNAPSHOT.jar
```

默认地址为 `http://localhost:8080`，通过 `ENDURANCE_PORT` 调整端口。默认 bootstrap profile 无需数据库、模型 Key 或 Docker 服务。

- `/`：静态服务说明页，对应 `endurance-server/src/main/resources/static/index.html`。
- `/api/system/info`：工程信息与尚未启用的能力状态，供前端检查连接。
- `/actuator/health`：服务健康检查。

上述只读入口允许匿名访问，其余请求默认拒绝。当前不提供默认账号；登录和管理员权限业务尚未实现。

## 外部服务配置

PostgreSQL 和 Flyway 依赖已纳入工程。`application-postgres.yml` 预留真实接入配置，但当前没有业务表结构或迁移脚本。准备好数据库及迁移设计后，才使用 `postgres` profile 并设置 `ENDURANCE_DB_URL`、`ENDURANCE_DB_USERNAME`、`ENDURANCE_DB_PASSWORD`。

在 IDEA 运行配置或 shell 中设置环境变量，也可以使用 Spring Boot 支持的外部 YAML/properties 配置：

| 环境变量 | 用途 | 默认值或要求 |
| --- | --- | --- |
| `ENDURANCE_PORT` | HTTP 服务端口 | 默认 `8080` |
| `ENDURANCE_DB_URL` | PostgreSQL JDBC 连接地址 | `postgres` profile 必填，例如 `jdbc:postgresql://localhost:5432/endurance` |
| `ENDURANCE_DB_USERNAME` | 数据库用户名 | `postgres` profile 必填 |
| `ENDURANCE_DB_PASSWORD` | 数据库密码 | `postgres` profile 必填，不提供默认密码 |

Spring AI 只使用低层库，当前不配置提供方或发起模型请求。Loop、工具、记忆、数据库实体与沙箱 Provider 尚未实现，不会在启动时创建容器或初始化模型。

## 版本管理

父 POM 统一管理内部模块、Spring AI、MyBatis-Plus、MCP 和 Docker Java 版本。API/Common/Core 保持与具体基础设施解耦；Maven 模块边界不表示必须拆成多个部署服务。

## 第一阶段实施与验收

按以下顺序实施，每项通过验证后单独提交，再进入下一项：

1. 工程边界与组件装配：统一目录、DTO 归属和模块依赖规则，增加架构验证。
2. 配置体系：类型化配置、启动校验、可选基础设施和环境变量约定。
3. HTTP 基础：注解校验、统一响应、错误码、自定义异常、全局处理和 Jackson 工具。
4. 日志与链路追踪：Log4j2 分类轮转、SLF4J、Trace/Span、OTLP 可选导出。
5. 资源与生命周期：统一执行器入口、上下文传播、线程池指标、超时与停机。
6. 数据访问：PostgreSQL/Redis 可选接入、MyBatis-Plus 插件、迁移与真实连接验证。
7. 访问控制：统一认证入口、错误响应、公开入口和管理端点边界。
8. 综合验收与运行说明：端到端验证、配置示例、部署和故障排查说明。

当前第 1 项已落实：HTTP DTO 独立放入 `server.web.dto`，目录规则见 AGENTS.md，架构约束由生产字节码测试维护。Agent Loop、模型调用和业务表仍未实现。

第 2 项已落实：`endurance.service.*` 使用类型化配置及 Bean Validation 校验，时钟统一使用 UTC。选择 `test` 等显式环境不会意外连接数据库。环境配置使用 `application-<环境>.yml` 或 Spring 支持的外部配置覆盖；模型、数据库和 Redis 后续通过独立开关启用，启用时才校验凭据。

本地凭据使用环境变量或仓库忽略的 `.env.local`；Spring 不会自动读取该文件，需在 shell 中导出或通过 IDEA 的运行配置注入。不要提交真实凭据，也不要把配置对象整体记录到日志。
