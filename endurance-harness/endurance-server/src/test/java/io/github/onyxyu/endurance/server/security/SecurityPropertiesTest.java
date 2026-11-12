package io.github.onyxyu.endurance.server.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SecurityPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesOnly.class)
            .withPropertyValues("endurance.security.enabled=false");

    @Test
    void disabledAuthenticationNeedsNoDefaultCredential() {
        runner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void enabledAuthenticationRejectsMissingOrWeakTokens() {
        runner.withPropertyValues("endurance.security.enabled=true").run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("endurance.security.enabled=true", "endurance.security.bearer-token=short")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("endurance.security.allowed-origins[0]=*")
                .run(context -> assertThat(context).hasFailed());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecurityProperties.class)
    static class PropertiesOnly {
    }
}
