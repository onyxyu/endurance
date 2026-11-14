package io.github.onyxyu.endurance.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.onyxyu.endurance.server.config.properties.ServiceProperties;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ServiceConfigurationTest {
    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ApplicationConfiguration.class)
            .withPropertyValues(
                    "endurance.service.name=Endurance Harness",
                    "endurance.service.version=test",
                    "endurance.service.stage=foundation"
            );

    @Test
    void bindsOverridesAndUsesUtcClock() {
        applicationContextRunner.withPropertyValues("endurance.service.name=Custom Harness").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ServiceProperties.class).name()).isEqualTo("Custom Harness");
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
        });
    }

    @Test
    void rejectsBlankRequiredConfiguration() {
        applicationContextRunner.withPropertyValues("endurance.service.name= ").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("NotBlank")
                    .hasStackTraceContaining("endurance.service.name");
        });
    }
}
