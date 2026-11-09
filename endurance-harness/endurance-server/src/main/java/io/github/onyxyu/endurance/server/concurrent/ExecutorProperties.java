package io.github.onyxyu.endurance.server.concurrent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("endurance.executors")
public record ExecutorProperties(@NotNull @Valid Pool io, @NotNull @Valid Pool cpu,
        @Min(1) @Max(120) int shutdownSeconds, @Min(1) int taskTimeoutSeconds) {
    public record Pool(@Min(1) @Max(256) int coreSize, @Min(1) @Max(1024) int maxSize,
            @Min(0) @Max(100000) int queueCapacity, @Min(1) int keepAliveSeconds) {
        @AssertTrue(message = "max-size must be at least core-size")
        public boolean isValidSizeRange() { return maxSize >= coreSize; }
    }
}
