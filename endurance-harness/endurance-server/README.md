# endurance-server

服务入口与基础设施。

当前提供 Spring Boot 显式装配、统一响应与注解校验、全局异常与 Common JSON 工具的装配、Log4j2/OTLP、Trace/MDC、执行器和可选服务级 Bearer 认证。公开只读入口及受保护指标入口见后端 README；没有用户账号、会话或 Agent 业务。

构建、配置、运行和验证见[后端 README](../README.md)，开发规则见[后端 AGENTS.md](../AGENTS.md)。

HTTP 请求归 `web.dto.request`，响应归 `web.dto.response`，边界转换归 `web.assembler`。内部用例契约归 API，不允许 Core 反向依赖这些 Web 类型。通用执行接口归 API，当前线程池和可观测实现由本模块装配。
