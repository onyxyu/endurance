package io.github.onyxyu.endurance.server.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 显式选择真实服务；所有表位于随机 schema，Redis 只操作专用带 TTL 的 key。
 */
@EnabledIfEnvironmentVariable(named = "ENDURANCE_INFRA_TESTS", matches = "true")
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "endurance.security.enabled=false", "endurance.postgres.enabled=true", "endurance.redis.enabled=true", "endurance.postgres.max-page-size=3",
        "spring.flyway.enabled=true", "spring.flyway.locations=classpath:db/verification"})
@Import(InfrastructureIntegrationTest.ProbeConfiguration.class)
class InfrastructureIntegrationTest {
    private static final String SCHEMA = "endurance_verify_" + UUID.randomUUID().toString().replace("-", "");
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ProbeMapper mapper;
    @Autowired
    StringRedisTemplate redis;
    @Autowired
    Flyway flyway;
    @Autowired
    PlatformTransactionManager transactions;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("endurance.postgres.schema", () -> SCHEMA);
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Test
    void postgresMigrationPaginationOptimisticLockAndWriteGuardWorkTogether() {
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("1");
        jdbc.update("insert into foundation_probe select i, 0, 'row-' || i from generate_series(1, 10) i");
        Page<Probe> page = mapper.selectPage(new Page<Probe>(1, 100), new QueryWrapper<Probe>().orderByAsc("id"));
        assertThat(page.getRecords()).hasSize(3);
        assertThat(page.getTotal()).isEqualTo(10);
        assertThat(mapper.selectPage(new Page<Probe>(2, 2), new QueryWrapper<Probe>().orderByAsc("id"))
                .getRecords()).extracting(Probe::getId).containsExactly(3L, 4L);
        Probe first = mapper.selectById(1L);
        Probe stale = mapper.selectById(1L);
        first.setPayload("updated");
        assertThat(mapper.updateById(first)).isEqualTo(1);
        stale.setPayload("stale");
        assertThat(mapper.updateById(stale)).isZero();
        assertThat(mapper.selectById(1L).getPayload()).isEqualTo("updated");
        assertThatThrownBy(() -> mapper.delete(new QueryWrapper<>())).isInstanceOf(RuntimeException.class);
        assertThat(mapper.selectCount(null)).isEqualTo(10);
    }

    @Test
    void transactionsRollBackAndRedisRoundTripsWithoutTouchingOtherKeys() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbc.update("insert into foundation_probe values (9999, 0, 'rollback')");
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("select count(*) from foundation_probe where id=9999", Long.class)).isZero();
        String key = "endurance:verification:" + UUID.randomUUID();
        try {
            redis.opsForValue().set(key, "verified", Duration.ofSeconds(30));
            assertThat(redis.opsForValue().get(key)).isEqualTo("verified");
            assertThat(redis.getExpire(key)).isBetween(1L, 30L);
        } finally {
            redis.delete(key);
        }
    }

    @AfterAll
    static void cleanup(@Autowired DataSource source) throws Exception {
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA \"" + SCHEMA + "\" CASCADE");
        }
    }

    @Data
    @TableName("foundation_probe")
    public static class Probe {
        @TableId(type = IdType.INPUT)
        private Long id;
        @Version
        private Integer version;
        private String payload;
    }

    @Mapper
    interface ProbeMapper extends BaseMapper<Probe> {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @MapperScan(basePackageClasses = InfrastructureIntegrationTest.class, annotationClass = Mapper.class)
    static class ProbeConfiguration {
    }
}
