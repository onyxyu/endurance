# endurance-api

运行与扩展契约模块。

- 职责：Java API、消息/运行类型与 Provider/Repository/Hook 扩展接口。
- 边界：接口签名尚待设计；保持框架与基础设施无关。

当前提供 `concurrent.TaskDispatcher` 和 `TaskPool` 作为各模块共享的纯 Java 任务契约。运行用例的 Command/Query/Result 后续按功能组织，不能依赖 Server 的 HTTP DTO。统一构建与运行方式见[后端 README](../README.md)。
