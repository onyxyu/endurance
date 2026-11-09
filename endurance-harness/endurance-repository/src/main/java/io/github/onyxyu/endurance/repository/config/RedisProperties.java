package io.github.onyxyu.endurance.repository.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("endurance.redis")
public record RedisProperties(@NotBlank String host, @Min(1) @Max(65535) int port,
        @Min(0) int database, String username, String password,
        @Min(1) long timeoutMs, boolean ssl) {
    @Override public String toString() { return "RedisProperties[credentials=REDACTED]"; }
}
