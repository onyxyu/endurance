package io.github.onyxyu.endurance.server.config;

import io.github.onyxyu.endurance.common.error.ErrorCode;
import io.github.onyxyu.endurance.server.security.SecurityErrorWriter;
import io.github.onyxyu.endurance.server.security.SecurityProperties;
import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableMethodSecurity
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, OpaqueTokenIntrospector introspector,
            SecurityErrorWriter errors,
            @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource) throws Exception {
        org.springframework.security.web.AuthenticationEntryPoint unauthorized = (request, response, failure) -> {
            response.setHeader("WWW-Authenticate", "Bearer realm=\"endurance\"");
            errors.write(response, ErrorCode.UNAUTHENTICATED);
        };
        org.springframework.security.web.access.AccessDeniedHandler forbidden = (request, response, failure) ->
                errors.write(response, ErrorCode.FORBIDDEN);
        var corsFilter = new org.springframework.web.filter.CorsFilter(corsSource);
        corsFilter.setCorsProcessor(new org.springframework.web.cors.DefaultCorsProcessor() {
            @Override
            protected void rejectRequest(org.springframework.http.server.ServerHttpResponse response) throws java.io.IOException {
                if (response instanceof org.springframework.http.server.ServletServerHttpResponse servlet) {
                    errors.write(servlet.getServletResponse(), ErrorCode.FORBIDDEN);
                } else { super.rejectRequest(response); }
            }
        });
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                // 只接受 Authorization Bearer，未使用 Cookie、Session 或表单认证。
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .addFilter(corsFilter)
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/api/system/info", "/actuator/health").permitAll()
                        .requestMatchers("/api/**").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.GET, "/actuator/metrics", "/actuator/metrics/**").hasRole("SERVICE")
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .oauth2ResourceServer(resource -> resource.opaqueToken(opaque -> opaque.introspector(introspector))
                        .authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .build();
    }

    /** 固定服务凭据的最小实现，未来可替换为标准 token introspection。 */
    @Bean
    @ConditionalOnMissingBean(OpaqueTokenIntrospector.class)
    OpaqueTokenIntrospector serviceTokenIntrospector(SecurityProperties properties) {
        byte[] expected = properties.bearerToken() == null ? new byte[0] : properties.bearerToken().getBytes(StandardCharsets.UTF_8);
        return token -> {
            if (!properties.enabled() || !MessageDigest.isEqual(expected, token.getBytes(StandardCharsets.UTF_8))) {
                throw new BadOpaqueTokenException("Invalid service credential");
            }
            return new DefaultOAuth2AuthenticatedPrincipal("service", Map.of("sub", "service"),
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "traceparent", "tracestate"));
        cors.setExposedHeaders(List.of("X-Trace-Id", "X-Span-Id"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    /** 抑制自动生成用户与密码；账号业务在后续设计。 */
    @Bean
    InMemoryUserDetailsManager userDetailsService() { return new InMemoryUserDetailsManager(); }
}
