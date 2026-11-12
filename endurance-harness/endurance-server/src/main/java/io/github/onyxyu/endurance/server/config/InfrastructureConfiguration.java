package io.github.onyxyu.endurance.server.config;

import io.github.onyxyu.endurance.repository.config.PostgresConfiguration;
import io.github.onyxyu.endurance.repository.config.RedisConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 跨模块能力在这里显式导入；每个能力自行按配置开关装配。
 */
@Configuration(proxyBeanMethods = false)
@Import({PostgresConfiguration.class, RedisConfiguration.class})
public class InfrastructureConfiguration {
}
