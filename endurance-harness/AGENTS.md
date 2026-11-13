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

## 当前服务基础

默认 bootstrap profile 提供服务说明 HTML、`/api/system/info` 和 `/actuator/health`，无需模型 Key、PostgreSQL 或 Docker 服务。其他后端请求默认拒绝，没有默认管理员账号。

第一阶段已实现统一响应、注解校验、异常与 JSON、Log4j2、OpenTelemetry/OTLP、执行器、可选 PostgreSQL/Redis、MyBatis-Plus 插件及服务级 Bearer 认证。模型接入、Agent Loop、真实会话、Hook、记忆、数据库业务、沙箱执行和用户账号认证尚待实现。依赖声明不代表能力已完成，按新增功能同步更新 README 和验证结果。

## 验证与运行

在本目录运行 `./mvnw verify` 或 `mvn verify`。涉及普通 Java 改动时运行相应模块和必要的集成验证；仅调整 POM 空行时，对比 XML 语义并运行 `mvn validate`。

启动方式和环境变量见 README，默认端口为 `8080`。运行或停止服务前核对命令、端口和进程，不影响其他工程。`target/`、日志、数据库凭据和本地配置不入库。

## 包与目录标准

- 包前缀固定为 `io.github.onyxyu.endurance.<模块名>`，生产代码和测试目录对应。
- Server 按技术边界组织：`config` 放装配、`web` 放控制器、`web.dto.request` 放 HTTP 请求、`web.dto.response` 放 HTTP 响应、`web.assembler` 放边界转换、`web.error` 放错误映射、`security` 放访问控制、`observability` 放追踪与日志、`concurrent` 放执行器。
- Repository 的 `config`、`entity`、`mapper` 分别放数据库装配、持久化实体和 Mapper；数据库迁移归本模块资源目录。
- Common 只放领域无关的基础类型；运行 API/SPI 归 Api。HTTP 响应包装与 Spring Bean 装配归 Server。通用 JSON 工具归 `common.json`，该包允许依赖 Jackson，但不得依赖 Spring；API/Core 不直接引用 Jackson 类型。
- 能力模块通过 Api 契约协作，由 Server 显式装配跨模块配置；禁止扩大扫描范围来隐式发现所有模块组件。
- Java 使用 4 空格缩进；导入分静态、普通两组，各组按字母排序，生产代码使用显式导入。
- 新增代码遵循构造器注入；配置类使用 `@Configuration(proxyBeanMethods = false)`。依赖版本集中到父 POM，保留语义分块。
- `ModuleArchitectureTest` 验证生产字节码的基础模块依赖、能力隔离和模块无环规则。

## 基础能力使用规则

- 请求 DTO 使用 `@Valid` 与 Jakarta Validation 注解；普通 API 返回 `ApiResponse<T>`，注入 `Responses` 快速构造。应用错误用 `ErrorCode`/`ApplicationException`，HTTP 映射只在 Server 完成。
- JSON 注入 `common.json.Json`，复用 Server 装配的 Spring Jackson 3 Mapper。不要创建第二套 Mapper或静态容器查找；Common 中仅 `json` 包允许依赖 Jackson，API/Core 保持其类型签名中立。
- 生产日志用 Lombok `@Slf4j`；socket 类别使用 `@Slf4j(topic = "endurance.socket")`。不记录认证头、完整请求体或配置凭据，不将 Trace ID 作为指标标签。
- 跨模块异步工作依赖 API 的 `TaskDispatcher`，由 Server 的 `Tasks` 实现；明确 IO/CPU 池及期限；`@Async` 仅通过 Spring 代理调用。保持追踪/MDC 的作用域清理，任务响应中断。不要使用无显式执行器的 `*Async` 或自行创建公共池。
- 默认启动与常规测试不依赖外部服务；真实 PG/Redis 用例显式设置 `ENDURANCE_INFRA_TESTS=true`。生产迁移不得混入测试表，禁止对既有 Redis 执行 FLUSH。
- 服务级 Bearer 认证不代表用户登录已实现；后续 Cookie/Session 认证需重新设计 CSRF。新增公开入口须明确声明并验证，管理端点维持最小暴露。
- 最终验收使用 `mvn clean verify`，并按需运行生产 JAR 冒烟脚本。每个实施项验证后按当前授权单独提交，提交与推送分别授权。

## Java 可读性规范

- 禁止使用 `var`。局部变量、循环变量和 try-with-resources 变量均显式声明类型。
- 使用类型前显式 import；类体内不以内联全限定类名声明类型、注解或静态调用。配置中的类名字符串不属于此限制。
- 方法、构造器和控制结构使用完整的多行代码块；即使只有一条语句也不省略大括号，不把整个方法、if、try/catch 或 lambda 语句块压成一行。
- 参数校验、创建对象、配置对象、执行逻辑、结果处理和资源释放等语义阶段之间适当空行，同组操作紧凑排列。
- 在关键步骤添加几个字的中文短注释，说明意图或原因，避免机械复述代码或模板化注释。
- Java 格式以父 POM 的 Spotless/Eclipse JDT 和 `config/java-formatter.xml` 为准，`.editorconfig` 提供 IDE 基础提示。修改 Java 文件后使用 `mvn spotless:apply`（可带 `-pl` 选择模块）；不要另用不同的默认格式化引擎覆写。只处理 Java，不能覆盖 POM 语义分组。格式化不能代替类型选择、命名、注释和语义分组检查。

## 传输类型与内部契约

- Controller 只接收 `server.web.dto.request`，只返回 `server.web.dto.response`；边界转换放 `server.web.assembler`。
- 运行用例的 Command/Query/Result 和能力 SPI 按功能归 `api.<功能>`，由 Core 实现。Controller 将 HTTP 请求转换为 API 契约后调用，不能把 Web DTO 传入 Core。
- Core 的内部状态与上下文仅供核心实现使用，不作为 Web DTO 或持久化实体；Repository Entity 不外泄。
- 通用任务的接口与池标识归 `api.concurrent`，Spring 线程池配置、MDC/Observation 传播和应用关闭适配留在 Server。Core 只通过接口调度，不依赖 Server 实现。

## 格式与规则检查

- 父 POM 统一配置 Spotless 和 Checkstyle，覆盖生产与测试 Java；`mvn validate`、`test`、`verify` 均先执行检查，违规即失败，检查过程不自动改写文件。
- `mvn spotless:apply` 显式修复格式；`mvn spotless:check` 只检查精确格式；`mvn checkstyle:check` 只检查结构与声明规则。禁止通过压缩单行结构、var 或内联全类名绕开规则。
- 规则文件集中位于本工程 `config`，版本集中在父 POM；新增模块继承配置，不复制规则，也不将构建插件当作运行 SDK 引入。
- 修改规则后验证正常代码可通过，故意违规代码会失败，重复格式化结果稳定。验证用的临时 Java 文件须清理，不进入提交。
