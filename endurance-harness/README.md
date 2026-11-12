# Endurance Harness

个人自用的云端 Agent 运行时后端。Java 21、Spring Boot 4.1.2。第一阶段服务基础设施已完成，Agent Loop、模型调用和业务数据尚未实现。

IDEA 直接打开本目录的 `pom.xml`。开发前读取 [AGENTS.md](AGENTS.md) 和涉及模块的 README；安装、构建和运行都在本目录执行。

## 工程与目录

| Maven 模块 | 职责 |
| --- | --- |
| endurance-common | 框架无关的通用基础类型、错误码、应用异常 |
| endurance-api | 后续运行类型与 API/SPI 契约 |
| endurance-core | 后续自研 Loop、上下文、生命周期与运行预算 |
| endurance-models | 后续 Spring AI 低层模型适配 |
| endurance-tools | 后续内置工具与 MCP 接入 |
| endurance-memory | 后续记忆写入、检索、整合与遗忘 |
| endurance-sandbox | 后续执行环境、命令及文件操作 |
| endurance-repository | 可选 PostgreSQL/Redis、MyBatis-Plus、Flyway、数据访问实现 |
| endurance-server | Spring Boot 装配、HTTP、访问控制、日志追踪、执行器和 JSON 适配 |

包前缀为 `io.github.onyxyu.endurance.<模块名>`。Server 按 `config`、`web`、`web.dto.request`、`web.dto.response`、`web.assembler`、`web.error`、`security`、`observability`、`concurrent` 划分。Repository 按 `config`、`entity`、`mapper` 划分，迁移放在本模块 `src/main/resources/db/migration`。未实现的目录不预建空类。

Common/API/Core 不依赖 Spring、数据库或模型 SDK；通用 JSON 工具集中在 Common 的 `json` 包，只有该包允许依赖 Jackson，API/Core 不直接绑定 Jackson 类型；能力实现通过 API 协作，由 Server 显式导入配置。HTTP DTO 不作为持久化实体。生产字节码的模块依赖和循环依赖由 `ModuleArchitectureTest` 检查。具体依赖版本以父 POM 为准，保留父 POM 的语义分块格式。

## 构建与启动

需要 Java 21、Maven 3.9.x。Maven Wrapper 固定 3.9.16：macOS/Linux 使用 `./mvnw`，Windows 使用 `mvnw.cmd`；已有 Maven 时可以直接使用 `mvn`。

```bash
mvn clean verify
java -jar endurance-server/target/endurance-server-0.1.0-SNAPSHOT.jar
```

默认端口为 8080，默认 profile 为 `bootstrap`。选择其他环境也不会自动启用外部服务：数据库、Redis、服务认证和 OTLP 导出各自有独立开关。默认启动不需要模型 Key、数据库、Redis、Docker 或观测平台。

需要在 IDEA 或 Maven 中调试时，也可以先运行 `mvn install -DskipTests`，再执行 `mvn -pl endurance-server spring-boot:run`。

| 入口 | 访问方式 | 响应 |
| --- | --- | --- |
| GET `/`、`/index.html` | 公开 | 静态服务说明 HTML |
| GET `/api/system/info` | 公开 | `ApiResponse<SystemInfoResponse>`，信息在 `data` 内 |
| GET `/actuator/health` | 公开 | 原生 Actuator 健康状态，不公开细节 |
| GET `/actuator/metrics`、`/actuator/metrics/**` | SERVICE 权限 | 原生 Actuator 指标 |
| 其他 `/api/**` | SERVICE 权限 | 普通接口统一包装；当前未实现的 API 返回 404 |
| 其他路径及管理端点 | 默认拒绝 | 401 或 403 |

## 配置与本地外部服务

服务元数据使用经过注解校验的 `endurance.service.*`；功能配置分别归 `endurance.executors`、`endurance.postgres`、`endurance.redis`、`endurance.security`。启用的配置不合法会阻止启动，未启用的外部服务不要求凭据。

环境覆盖使用 Spring 支持的外部 YAML/properties、环境变量或 IDEA 运行配置。本地变量示例见 [.env.example](.env.example)。首次使用且 `.env.local` 不存在时复制示例并填写；真实凭据只放在 Git 忽略的 `.env.local`，不要覆盖已有本地配置。

Spring 不自动读取 `.env.local`。在 shell 中加载后运行：

```bash
set -a
source .env.local
set +a
java -jar endurance-server/target/endurance-server-0.1.0-SNAPSHOT.jar
```

| 环境变量 | 默认值 / 说明 |
| --- | --- |
| `ENDURANCE_PORT` | `8080`；测试可用 `0` 分配空闲端口 |
| `ENDURANCE_LOG_PATH` | `logs` |
| `ENDURANCE_SHUTDOWN_SECONDS` | `5`，停机阶段及执行器等待时限 |
| `ENDURANCE_POSTGRES_ENABLED` | `false` |
| `ENDURANCE_DB_URL/USERNAME/PASSWORD` | 启用 PostgreSQL 时必填，URL 为 PostgreSQL JDBC URL |
| `ENDURANCE_DB_SCHEMA` | `endurance`，限字母、数字和下划线，不以数字开头 |
| `ENDURANCE_REDIS_ENABLED` | `false` |
| `ENDURANCE_REDIS_HOST/PORT/DATABASE` | `127.0.0.1` / `6379` / `0` |
| `ENDURANCE_REDIS_USERNAME/PASSWORD/SSL` | 可选凭据 / `false` |
| `ENDURANCE_AUTH_ENABLED` | `false`，关闭时受保护入口均不可访问 |
| `ENDURANCE_API_TOKEN` | 启用服务认证时必填，32~256 个非空白字符 |
| `ENDURANCE_TRACE_SAMPLING` | `1.0`，默认保留所有请求的采样决定；上游采样决定优先 |
| `ENDURANCE_OTLP_TRACES_ENABLED` | `false` |
| `ENDURANCE_OTLP_METRICS_ENABLED` | `false` |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | `http://127.0.0.1:4318/v1/traces` |
| `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | `http://127.0.0.1:4318/v1/metrics` |

PostgreSQL 使用 HikariCP，默认最大连接 10、最小空闲 1、连接超时 3000 ms。Flyway 仅在 PostgreSQL 启用时执行，创建指定 schema 和迁移历史，禁止 clean 和隐式 baseline。当前没有生产业务表或迁移脚本，pgvector 的业务用途仍待后续设计。

MyBatis-Plus 配置乐观锁、全表更新/删除拦截、PostgreSQL 分页；分页最后执行，默认单页上限 200，通过 `endurance.postgres.max-page-size` 调整。Mapper 位于 `repository.mapper` 且标注 `@Mapper`，排序列由服务端控制。Redis 使用 Lettuce 和 `StringRedisTemplate`，配置 SSL、认证、数据库编号与超时，当前不实现缓存、分布式锁、会话或消息队列业务。

## HTTP 响应、校验和 JSON

普通 JSON API 统一返回 `ApiResponse<T>`：

```json
{
  "success": true,
  "code": "OK",
  "message": "Success",
  "data": {},
  "errors": [],
  "traceId": "0123456789abcdef0123456789abcdef",
  "timestamp": "2026-11-09T00:00:00Z"
}
```

通过构造器注入 `Responses`，使用 `responses.ok(data)`、`responses.error(code)` 快速创建。`ErrorCode` 与 `ApplicationException` 位于 Common，响应包装位于 `server.web.dto.response`，HTTP 映射和全局异常处理位于 Server。`ApplicationException` 的消息必须可公开；未知异常只对外返回固定消息，日志保留堆栈。

| 错误码 | HTTP 状态 |
| --- | --- |
| `OK` | 200 |
| `REQUEST_INVALID` | 400 |
| `AUTH_REQUIRED` | 401 |
| `ACCESS_DENIED` | 403 |
| `RESOURCE_NOT_FOUND` | 404 |
| `METHOD_NOT_ALLOWED` | 405，保留 Allow 头 |
| `RESPONSE_NOT_ACCEPTABLE` | 406 |
| `RESOURCE_CONFLICT` | 409 |
| `MEDIA_TYPE_UNSUPPORTED` | 415 |
| `RATE_LIMITED` | 429 |
| `INTERNAL_ERROR` | 500 |
| `TASK_REJECTED`、`SERVICE_UNAVAILABLE` | 503 |
| `OPERATION_TIMEOUT` | 504 |

请求 DTO 使用 `@Valid` 配合 Jakarta Validation 注解；参数约束同样由框架校验。`errors` 为字段及消息列表，不包含被拒绝的值。错误 JSON 保持真实 HTTP 状态，过滤器、MVC、内容协商和跨域拒绝使用相同包装。

Jackson 使用 Spring 管理的 Jackson 3 `JsonMapper`，时间统一为 ISO-8601，公共时钟为 UTC。注入 `common.json.Json` 后使用 `write`、`read`（Class 或 TypeReference）、`readList`、`readMap`、`convert`、`tree/toTree/fromTree`，不自行创建 Mapper 或静态访问 Spring 容器。转换异常不在外层消息中暴露原始载荷。

静态资源、Actuator 和未来 SSE 不做 JSON 包装。`/api/system/info` 的原有字段已移动到 `data` 内，前端调用方后续需要解包；本阶段没有修改前端。

## 日志、追踪与 OTLP

Log4j2 为唯一日志实现，生产代码通过 Lombok `@Slf4j` 使用 SLF4J。格式包含 UTC 时间、级别、线程、logger、Trace ID 和 Span ID。

| 文件 | 内容 | 归档保留 |
| --- | --- | --- |
| `info.log` | INFO/WARN | 14 天 |
| `error.log` | ERROR/FATAL 及异常堆栈 | 30 天 |
| `socket.log` | 专用 `endurance.socket` logger | 7 天 |

均按每日及单文件 50 MB 轮转、压缩归档。socket 分类用于未来长连接事件，当前没有 WebSocket 服务。日志文件和归档由文件系统权限及部署存储配额管理。

HTTP 入口复用 Micrometer/OpenTelemetry 的 W3C `traceparent`，不信任任意客户端 `X-Trace-Id`。MDC、响应体和 `X-Trace-Id`/`X-Span-Id` 响应头使用同一追踪上下文。访问日志记录方法、匹配的路由模板、状态和耗时，不记录 query、请求体、认证头或配置凭据。出站 HTTP 使用 Spring 管理的 `RestClient.Builder` 等构造器传播追踪。

默认不向观测平台发送数据；分别启用 Trace 和指标开关后，使用 OTLP HTTP/protobuf 对接兼容 Collector。端点如上表，认证头使用 Spring 对应 exporter 的 `headers` 配置，不写入源码。文件日志由 Collector 文件采集器接入平台，当前没有启用 OTLP 日志 appender。协议验收使用本地 HTTP 接收器，未绑定特定观测厂商。

## 执行器与生命周期

所有线程池在 `ExecutorConfiguration` 装配，集中配置于 `endurance.executors.*`：

| 池 | 核心 / 最大线程 | 队列容量 | 保活 |
| --- | --- | --- | --- |
| IO：`ioExecutor` | 4 / 16 | 256 | 60 秒 |
| CPU：`cpuExecutor` | 2 / 4 | 64 | 60 秒 |
| 到期调度：`timeoutScheduler` | 1 | 仅为未完成任务保留到期检查 | 完成或取消后移除 |

跨模块依赖 API 的 `TaskDispatcher`，使用 `submit(TaskPool.IO, callable)`、`execute(TaskPool.CPU, runnable)`。Server 的 `Tasks` 提供带追踪的实现，Core/Memory/Tools 不依赖 Server。返回可取消的 `CompletableFuture`，可指定 `Duration`，默认期限为 30 秒并包含排队时间。容量耗尽明确拒绝，不在 HTTP 调用线程执行，也不静默丢弃。`@Async` 默认使用 IO 池，可指定 `@Async("cpuExecutor")`；其本身不提供 Tasks 的期限 API。

任务传播 Micrometer 上下文及 MDC，执行时创建子 Observation/Span，结束后恢复工作线程上下文。`@Async` 需要通过 Spring 代理调用，同类内部调用不经过代理。禁止无显式执行器的 `CompletableFuture.*Async` 或自行创建公共线程池；身份和业务上下文通过显式参数传递。

Actuator 自动绑定 `executor.active/queued/queue.remaining/pool.core/pool.max/pool.size/completed` 等指标，`name` 标签为池名。任务耗时为 `endurance.executor.task`，拒绝次数为 `endurance.executor.rejected`；指标可自动随 OTLP 导出。不要把 Trace ID、用户 ID 等高基数字段作为指标标签。

关闭事件先阻止新提交，再取消 Tasks 的未完成 Future 并中断底层任务，避免排队任务永久悬挂；执行器、调度器、连接池和客户端交由 Spring 关闭。任务实现必须响应中断，无法保证强行终止不合作的代码；后续沙箱执行将单独管理进程生命周期。

## 访问控制

使用 Spring Security 标准 Bearer 资源服务器过滤器和可替换的 `OpaqueTokenIntrospector`。当前实现是配置式服务凭据，校验采用常量时间比较，成功获得 SERVICE 权限。关闭认证时不会开放受保护入口，没有默认账号或默认密码。

凭据仅由 `Authorization: Bearer <token>` 提交，不从 query、Cookie 或 Session 获取；请求不建立登录 Session。过滤器错误带统一包装和 Trace，401 另带 Bearer challenge。方法级权限可用 `@PreAuthorize` 声明，跨域仅允许 `endurance.security.allowed-origins` 显式列出的 HTTP(S) 源，默认关闭跨域，不支持通配源或 Cookie 凭据。

这不是用户登录、管理员账号或多租户认证；不要把服务凭据内置在浏览器代码中。远程访问使用 TLS。当前只采用 Authorization 头认证，未使用 Cookie/Session，故关闭 CSRF；未来接入浏览器会话认证时必须重新设计 CSRF 策略。

## 验证与验收

第一阶段八项均按独立提交完成：模块与装配、配置、HTTP、日志追踪、执行器、数据访问、访问控制、综合验收与文档。

```bash
# 无外部依赖：31 个用例中 29 个运行，2 个真实基础设施用例跳过。
mvn clean verify

# 需要 Python 3.9+；启动生产 JAR、分配随机端口并验证 HTTP/日志/SIGTERM。
python3 scripts/smoke_test.py

# 加载本地配置后，额外运行 PostgreSQL/Redis 真正的集成验证。
set -a
source .env.local
set +a
ENDURANCE_INFRA_TESTS=true mvn clean verify
python3 scripts/smoke_test.py --local
```

真实集成验证要求本地 PostgreSQL 数据库存在并允许创建/删除测试 schema，Redis 凭据正确。测试在随机 `endurance_verify_*` schema 建临时表并清理，覆盖 Flyway、分页上限、乐观锁、全表写入拦截和事务回滚；Redis 仅操作带 TTL 的随机 `endurance:verification:*` key，不执行 FLUSH。生产 JAR 冒烟的 `--local` 模式使用 `.env.local` 的 PG/Redis 配置，以及仅本次运行有效的随机服务凭据。

其他用例覆盖架构依赖、配置校验、显式环境启动、注解校验、HTTP 错误、Jackson 泛型与时间转换、日志分流与时间轮转策略、W3C 追踪、真实 OTLP 导出、线程池上下文传播/饱和/取消/超时/关闭，以及认证、权限和 CORS。

配置失败、应用异常等负向测试会主动产生 WARN/ERROR 日志，以 Maven 用例结果为准。删除或重命名配置资源后使用 `clean verify`，防止旧 `target/classes` 文件影响增量构建。测试日志、报告、截图和本地配置不入库；冒烟失败日志保存在 `endurance-server/target/smoke-failure.log`。

常见问题：服务启动失败先查看必填配置与连接错误；401 检查认证开关和 Bearer 头；403 检查权限和显式跨域源；外部能力启用后健康检查失败需检查对应服务；没有观测平台时保持 OTLP 导出关闭。停止服务只操作当前工程的进程，避免按端口批量结束其他项目。

## 尚未实现

Agent Loop、运行/消息契约、模型提供方、真实会话、Queue/Steer、Hook、长期记忆、业务实体与表、MCP 工具、沙箱执行、用户和管理员登录均尚未实现。依赖或扩展点不代表这些能力已经完成。TARS 不在当前初始化范围内。

## 边界类型与代码格式

HTTP 请求、响应分别放在 `server.web.dto.request`、`server.web.dto.response`，请求与内部类型之间的转换放 `server.web.assembler`。目前只有工程信息响应，故只创建实际需要的响应类型，不预建空请求类。

后续运行接口按以下方向实现：HTTP Request → Server Assembler → API Command/Query → Core → API Result → Server Assembler → HTTP Response。Command/Query/Result 按功能放入 API，核心内部状态归 Core，持久化实体归 Repository。不能通过让 Core 引用 Server DTO 来复用类型；当前尚无 Agent 用例，示意方向不代表已有运行接口。

JSON 工具在 Common 中为普通 Java 类，由 Server 的 `JsonConfiguration` 使用 Spring 管理的 `JsonMapper` 创建 Bean。其他模块可以复用工具而不依赖 Server，Common 无需引用 Spring 注解。

Java 代码禁止 `var` 和类体内全限定类型名，控制结构、方法、构造器、lambda 语句块均采用完整多行格式；按语义分组留空行，并在关键步骤写简短中文注释。IDEA 使用项目 `.editorconfig`，不能把格式化等同于类型与代码设计检查。外部工具写入磁盘不等同于 IDEA 内部保存，IDEA 的保存时格式化不会因此自动触发。

本机 IDEA 的官方命令行格式化器已验证可用，可在独立临时 config/system 目录运行，避免与已打开的 IDE 争用实例。后续修改 Java 文件后对该文件调用相同 formatter，并人工检查显式类型、语义空行和短注释。仅处理 Java，不格式化 POM。IDE 内也可启用 Tools → Actions on Save → Reformat code / Optimize imports；外部修改磁盘文件不会自动触发该保存动作。
