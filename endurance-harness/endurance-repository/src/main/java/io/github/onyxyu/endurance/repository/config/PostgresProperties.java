package io.github.onyxyu.endurance.repository.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("endurance.postgres")
public record PostgresProperties(@NotBlank @Pattern(regexp = "jdbc:postgresql:.*") String url,
        @NotBlank String username, @NotBlank String password,
        @NotBlank @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_]*") String schema,
        @Min(1) int maxPoolSize, @Min(0) int minimumIdle, @Min(250) long connectionTimeoutMs,
        @Min(1) @Max(10000) int maxPageSize) {
    @AssertTrue(message = "minimum-idle must not exceed max-pool-size")
    public boolean isValidPoolRange() { return minimumIdle <= maxPoolSize; }
    @Override public String toString() { return "PostgresProperties[credentials=REDACTED]"; }
}
