package io.github.onyxyu.endurance.repository.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "endurance.redis", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfiguration {
    @Bean
    LettuceConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        // 服务地址与认证
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration(
                redisProperties.host(),
                redisProperties.port()
        );
        redisConfiguration.setDatabase(redisProperties.database());
        if (StringUtils.hasText(redisProperties.username())) {
            redisConfiguration.setUsername(redisProperties.username());
        }
        if (StringUtils.hasText(redisProperties.password())) {
            redisConfiguration.setPassword(redisProperties.password());
        }

        // 超时与连接选项
        LettuceClientConfigurationBuilder lettuceClientConfigurationBuilder = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisProperties.timeoutMs())).shutdownTimeout(Duration.ofSeconds(1));
        if (redisProperties.ssl()) {
            lettuceClientConfigurationBuilder.useSsl();
        }

        // 启动时验证连接
        LettuceConnectionFactory redisConnectionFactory = new LettuceConnectionFactory(
                redisConfiguration,
                lettuceClientConfigurationBuilder.build()
        );
        redisConnectionFactory.setEagerInitialization(true);
        return redisConnectionFactory;
    }

    @Bean
    StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
