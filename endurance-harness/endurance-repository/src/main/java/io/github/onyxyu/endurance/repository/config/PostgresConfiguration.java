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
    HikariDataSource dataSource(PostgresProperties properties) {
        var configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.url());
        configuration.setUsername(properties.username());
        configuration.setPassword(properties.password());
        configuration.setSchema(properties.schema());
        configuration.setPoolName("endurance-postgres");
        configuration.setMaximumPoolSize(properties.maxPoolSize());
        configuration.setMinimumIdle(properties.minimumIdle());
        configuration.setConnectionTimeout(properties.connectionTimeoutMs());
        configuration.setInitializationFailTimeout(properties.connectionTimeoutMs());
        return new HikariDataSource(configuration);
    }

    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor(PostgresProperties properties) {
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        var pagination = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        pagination.setMaxLimit((long) properties.maxPageSize());
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
