# 数据库迁移

此目录存放未来经过评审的 Flyway 业务迁移。当前不创建业务表；启用 PostgreSQL 时，Flyway 只维护配置 schema 的迁移历史。

真实基础设施验收的临时测试表位于 Server 测试资源 `db/verification`，不得放入生产迁移。
