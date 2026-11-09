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

PostgreSQL 和 Flyway 依赖已纳入工程。当前已提供可选 PostgreSQL 连接、连接池、事务、Flyway 和 MyBatis-Plus 插件；尚无业务表。设置 `ENDURANCE_POSTGRES_ENABLED=true`，并提供 `ENDURANCE_DB_URL`、`ENDURANCE_DB_USERNAME`、`ENDURANCE_DB_PASSWORD` 才启用；无需切换专用 profile。

在 IDEA 运行配置或 shell 中设置环境变量，也可以使用 Spring Boot 支持的外部 YAML/properties 配置：

| 环境变量 | 用途 | 默认值或要求 |
| --- | --- | --- |
| `ENDURANCE_PORT` | HTTP 服务端口 | 默认 `8080` |
| `ENDURANCE_DB_URL` | PostgreSQL JDBC 连接地址 | 启用 PostgreSQL 时必填，例如 `jdbc:postgresql://localhost:5432/endurance` |
| `ENDURANCE_DB_USERNAME` | 数据库用户名 | 启用 PostgreSQL 时必填 |
| `ENDURANCE_DB_PASSWORD` | 数据库密码 | 启用 PostgreSQL 时必填，不提供默认密码 |

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

第 3 项已落实：普通 `/api/**` JSON 返回 `ApiResponse<T>`，包含 `success/code/message/data/errors/traceId/timestamp`。使用注入的 `Responses.ok(data)`、`Responses.error(code)` 快速创建。`ErrorCode` 与 `ApplicationException` 在 Common，HTTP 状态映射与异常处理在 Server。请求 DTO 使用 `@Valid` 配合 Jakarta Validation 注解；约束失败不回显被拒绝的值。未知异常返回固定 500 响应，日志保留堆栈。

`/api/system/info` 的原有信息现放在 `data` 内，调用方需要解包；本阶段不修改前端。静态资源、Actuator 和未来的 SSE 不做 JSON 包装。时间使用带时区的 ISO-8601 表示。Jackson 工具通过构造器注入 `server.support.json.Json`，支持对象、泛型、List、Map、树之间的转换，复用 Spring 管理的 Jackson 3 `JsonMapper`，不自行创建全局 Mapper。

第 4 项已落实：Log4j2 为唯一日志实现，生产代码使用 Lombok `@Slf4j`。日志默认写入 `logs/`（`ENDURANCE_LOG_PATH` 可覆盖），包括 INFO/WARN 的 `info.log`、ERROR 的 `error.log` 和专用 `endurance.socket` logger 的 `socket.log`。三类日志每日及 50 MB 轮转，归档分别保留 14/30/7 天；socket 分类预留给未来长连接事件，当前没有实现 WebSocket 服务。

入站 HTTP 使用 Micrometer/OpenTelemetry 生成或延续 W3C `traceparent`，响应带 `X-Trace-Id`、`X-Span-Id`，响应体和 MDC 使用同一 Trace ID。不要信任客户端任意提供的 `X-Trace-Id`。日志不记录请求体、query、认证头或配置凭据。出站 HTTP 应使用 Spring 管理的 `RestClient.Builder` 等客户端构造器以传播追踪。

Trace 与指标默认在本地采集、不向外发送。连接任意兼容 OTLP 的 Collector 时，分别设置 `ENDURANCE_OTLP_TRACES_ENABLED=true`、`ENDURANCE_OTLP_METRICS_ENABLED=true`，并通过 `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`、`OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` 配置 HTTP/protobuf 端点（默认 4318 的 `/v1/traces`、`/v1/metrics`）。采样比例用 `ENDURANCE_TRACE_SAMPLING` 调整；导出认证头使用 Spring 对应 exporter 的 `headers` 配置。文件日志可由 Collector 文件采集器统一接入平台，当前不启用 OTLP 日志 appender。

第 5 项已落实：所有线程池在 `ExecutorConfiguration` 装配，通过 `endurance.executors.*` 统一调整 IO/CPU 池的核心数、上限、队列和保活时间。`Tasks.submit(TaskPool.IO, callable)`、`Tasks.execute(TaskPool.CPU, runnable)` 是推荐入口，返回可取消的 `CompletableFuture`；可传入 `Duration` 单独指定包含排队时间的期限，默认 30 秒。任务满载明确拒绝，不在 HTTP 线程执行，也不静默丢弃。

`@Async` 默认使用 IO 池，也可指定 `@Async("cpuExecutor")`。两者均传播 Micrometer 上下文和 MDC，执行时创建子 Observation/Span，结束后恢复工作线程原有上下文。禁止使用无显式执行器的 `CompletableFuture.*Async` 或自行创建公共线程池。身份和业务上下文应通过显式参数传递。

Actuator 自动绑定线程池的 `executor.active`、`executor.queued`、`executor.queue.remaining`、`executor.pool.core`、`executor.pool.max`、`executor.pool.size`、`executor.completed` 等指标，`name` 标签对应池名；任务耗时使用 `endurance.executor.task`，拒绝次数使用 `endurance.executor.rejected`。这些指标随 OTLP 指标导出，不使用 Trace ID 作为指标标签。默认停机时限为每阶段 5 秒，通过 `ENDURANCE_SHUTDOWN_SECONDS` 调整；任务入口在关闭事件时取消未完成任务并中断底层 Future，任务实现仍须响应中断。

第 6 项已落实：PostgreSQL 与 Redis 独立按开关启用，连接及插件配置归 Repository，由 Server 显式导入。PostgreSQL 默认 schema 为 `endurance`，Flyway 禁止 clean 与隐式 baseline；尚无生产业务迁移。MyBatis-Plus 配置 PostgreSQL 分页、单页上限 200、乐观锁和全表更新/删除拦截，分页插件最后执行。Mapper 放在 `repository.mapper`，须标注 `@Mapper`；实体与 Web DTO 分离。

Redis 使用 Lettuce 和 `StringRedisTemplate`，当前不引入缓存、锁、会话或队列业务。`ENDURANCE_REDIS_ENABLED` 默认 false；连接配置包括 `ENDURANCE_REDIS_HOST/PORT/DATABASE/USERNAME/PASSWORD/SSL`。启用后会尝试建立连接，配置与连接错误及时暴露。`.env.example` 提供变量说明；真实本地配置只放 `.env.local`，加载方式为在 shell 中执行 `set -a`、`source .env.local`、`set +a`。

默认 `mvn verify` 不依赖外部服务，真实基础设施验收额外设置 `ENDURANCE_INFRA_TESTS=true` 后运行。验收在随机 `endurance_verify_*` schema 建立临时测试表，检查 Flyway、分页上限、乐观锁、全表写入拦截及事务回滚，结束后删除该 schema；Redis 只操作带 TTL 的随机 `endurance:verification:*` key 并清理。不会执行 Redis FLUSH 或修改其他 schema。
