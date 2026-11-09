-- 仅用于独立 schema 的真实数据库验收，不打包到生产 JAR。
CREATE TABLE foundation_probe (
    id BIGINT PRIMARY KEY,
    version INTEGER NOT NULL DEFAULT 0,
    payload VARCHAR(100) NOT NULL
);
