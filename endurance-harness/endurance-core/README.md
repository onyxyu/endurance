# endurance-core

自研执行核心模块。

- 职责：Agent Loop、上下文、状态推进、Hook 生命周期与运行预算。
- 边界：通过 API 调用能力；Loop 尚未实现。

当前为工程骨架；具体能力按后续设计实现。统一构建与运行方式见[后端 README](../README.md)。

通用线程池不归本模块：Core 通过 API 的 `TaskDispatcher` 使用运行环境提供的执行器，负责 Agent 的排队、预算与状态调度；Spring 线程池、MDC 和应用关闭适配由 Server 装配。本模块不依赖 Server、Web DTO 或具体线程池实现。
