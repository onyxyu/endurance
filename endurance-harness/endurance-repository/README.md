# endurance-repository

数据访问实现。

当前提供按开关启用的 PostgreSQL/HikariCP、事务、Flyway、MyBatis-Plus 插件和 Redis/Lettuce 配置，由 Server 显式装配。Mapper 位于 `mapper` 并标注 `@Mapper`，实体归 `entity`；当前没有业务实体、Mapper 或业务表。生产迁移资源位于 `db/migration`，不包含测试表。

构建、配置、运行和验证见[后端 README](../README.md)，开发规则见[后端 AGENTS.md](../AGENTS.md)。
