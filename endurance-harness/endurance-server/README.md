# endurance-server

服务入口与基础设施。

当前提供 Spring Boot 显式装配、统一响应与注解校验、全局异常与 Jackson 工具、Log4j2/OTLP、Trace/MDC、执行器和可选服务级 Bearer 认证。公开只读入口及受保护指标入口见后端 README；没有用户账号、会话或 Agent 业务。

构建、配置、运行和验证见[后端 README](../README.md)，开发规则见[后端 AGENTS.md](../AGENTS.md)。
