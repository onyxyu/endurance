package io.github.onyxyu.endurance.repository.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "endurance.postgres", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PostgresProperties.class)
@MapperScan(basePackages = "io.github.onyxyu.endurance.repository.mapper", annotationClass = Mapper.class)
public class PostgresConfiguration {
    @Bean(destroyMethod = "close")
    HikariDataSource dataSource(PostgresProperties postgresProperties) {
        // 连接参数
        HikariConfig hikariConfiguration = new HikariConfig();
        hikariConfiguration.setJdbcUrl(postgresProperties.url());
        hikariConfiguration.setUsername(postgresProperties.username());
        hikariConfiguration.setPassword(postgresProperties.password());
        hikariConfiguration.setSchema(postgresProperties.schema());

        // 连接池参数
        hikariConfiguration.setPoolName("endurance-postgres");
        hikariConfiguration.setMaximumPoolSize(postgresProperties.maxPoolSize());
        hikariConfiguration.setMinimumIdle(postgresProperties.minimumIdle());
        hikariConfiguration.setConnectionTimeout(postgresProperties.connectionTimeoutMs());
        hikariConfiguration.setInitializationFailTimeout(postgresProperties.connectionTimeoutMs());

        return new HikariDataSource(hikariConfiguration);
    }

    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor(PostgresProperties postgresProperties) {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        // 写入保护
        mybatisPlusInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        mybatisPlusInterceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // 分页插件放最后
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        paginationInterceptor.setMaxLimit((long) postgresProperties.maxPageSize());
        paginationInterceptor.setOverflow(false);
        mybatisPlusInterceptor.addInnerInterceptor(paginationInterceptor);
        return mybatisPlusInterceptor;
    }
}
