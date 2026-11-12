# endurance-common

通用基础模块。

- `error`：框架无关错误码与应用异常。
- `json`：跨模块复用的 Jackson 3 转换工具和转换异常，普通 Java 类，不使用 Spring 注解；由 Server 使用 Spring 管理的 Mapper 装配。

仅 `json` 包允许依赖 Jackson；不放 HTTP DTO、Spring 配置、数据库或模型 SDK 类型。API/Core 不直接使用 Jackson 类型作为运行契约。

构建、运行和验证见[后端 README](../README.md)，开发规范见[AGENTS.md](../AGENTS.md)。
