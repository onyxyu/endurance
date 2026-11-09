package io.github.onyxyu.endurance.repository.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "endurance.redis", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfiguration {
    @Bean
    LettuceConnectionFactory redisConnectionFactory(RedisProperties properties) {
        var server = new RedisStandaloneConfiguration(properties.host(), properties.port());
        server.setDatabase(properties.database());
        if (StringUtils.hasText(properties.username())) { server.setUsername(properties.username()); }
        if (StringUtils.hasText(properties.password())) { server.setPassword(properties.password()); }
        var client = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(properties.timeoutMs()))
                .shutdownTimeout(Duration.ofSeconds(1));
        if (properties.ssl()) { client.useSsl(); }
        var factory = new LettuceConnectionFactory(server, client.build());
        factory.setEagerInitialization(true);
        return factory;
    }

    @Bean
    StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
