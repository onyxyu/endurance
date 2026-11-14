package io.github.onyxyu.endurance.server.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.onyxyu.endurance.repository.config.PostgresConfiguration;
import io.github.onyxyu.endurance.repository.config.RedisConfiguration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class InfrastructureConfigurationTest {
    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PostgresConfiguration.class, RedisConfiguration.class)
            .withPropertyValues(
                    "endurance.security.enabled=false",
                    "endurance.postgres.enabled=false",
                    "endurance.redis.enabled=false"
            );

    @Test
    void disabledInfrastructureDoesNotRequireCredentialsOrCreateClients() {
        applicationContextRunner.run(
                context -> assertThat(context).hasNotFailed().doesNotHaveBean(DataSource.class)
                        .doesNotHaveBean(RedisConnectionFactory.class)
        );
    }

    @Test
    void enabledServicesFailOnInvalidConfigurationBeforeConnecting() {
        applicationContextRunner.withPropertyValues("endurance.postgres.enabled=true")
                .run(context -> assertThat(context).hasFailed());
        applicationContextRunner.withPropertyValues(
                "endurance.redis.enabled=true",
                "endurance.redis.host=localhost",
                "endurance.redis.port=0",
                "endurance.redis.timeout-ms=1000"
        )
                .run(context -> assertThat(context).hasFailed());
    }
}
