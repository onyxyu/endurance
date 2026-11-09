package io.github.onyxyu.endurance.server.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 可公开的服务元数据；凭据不得放入此配置对象。 */
@Validated
@ConfigurationProperties("endurance.service")
public record ServiceProperties(@NotBlank String name, @NotBlank String version, @NotBlank String stage) {}
