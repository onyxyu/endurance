package io.github.onyxyu.endurance.server.config;

import io.github.onyxyu.endurance.common.json.Json;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * 装配公共 JSON 工具，Mapper 配置沿用 Spring。
 */
@Configuration(proxyBeanMethods = false)
public class JsonConfiguration {
    @Bean
    Json json(JsonMapper jsonMapper) {
        return new Json(jsonMapper);
    }
}
