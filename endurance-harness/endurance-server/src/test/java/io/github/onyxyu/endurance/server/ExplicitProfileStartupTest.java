package io.github.onyxyu.endurance.server;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * 选择部署环境不应意外启用外部服务。
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"endurance.security.enabled=false", "endurance.postgres.enabled=false", "endurance.redis.enabled=false"})
class ExplicitProfileStartupTest {
    @Autowired
    ApplicationContext context;

    @Test
    void explicitEnvironmentStillStartsWithoutDatabaseCredentials() {
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
    }
}
