package io.github.onyxyu.endurance.server.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.onyxyu.endurance.repository.config.PostgresConfiguration;
import io.github.onyxyu.endurance.repository.config.RedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class InfrastructureConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PostgresConfiguration.class, RedisConfiguration.class)
            .withPropertyValues("endurance.security.enabled=false", "endurance.postgres.enabled=false", "endurance.redis.enabled=false");

    @Test
    void disabledInfrastructureDoesNotRequireCredentialsOrCreateClients() {
        runner.run(context -> assertThat(context).hasNotFailed()
                .doesNotHaveBean(javax.sql.DataSource.class)
                .doesNotHaveBean(org.springframework.data.redis.connection.RedisConnectionFactory.class));
    }

    @Test
    void enabledServicesFailOnInvalidConfigurationBeforeConnecting() {
        runner.withPropertyValues("endurance.postgres.enabled=true").run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("endurance.redis.enabled=true", "endurance.redis.host=localhost",
                "endurance.redis.port=0", "endurance.redis.timeout-ms=1000")
                .run(context -> assertThat(context).hasFailed());
    }
}
