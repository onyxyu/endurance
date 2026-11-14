package io.github.onyxyu.endurance.server.config;

import io.github.onyxyu.endurance.common.error.ErrorCode;
import io.github.onyxyu.endurance.server.security.SecurityErrorWriter;
import io.github.onyxyu.endurance.server.security.SecurityProperties;
import jakarta.servlet.DispatcherType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@EnableMethodSecurity
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                            OpaqueTokenIntrospector opaqueTokenIntrospector,
                                            SecurityErrorWriter securityErrorWriter,
                                            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint = (request, response, failure) -> {
            response.setHeader("WWW-Authenticate", "Bearer realm=\"endurance\"");
            securityErrorWriter.write(response, ErrorCode.UNAUTHENTICATED);
        };
        AccessDeniedHandler accessDeniedHandler = (request, response, failure) -> securityErrorWriter
                .write(response, ErrorCode.FORBIDDEN);
        CorsFilter corsFilter = new CorsFilter(corsConfigurationSource);
        corsFilter.setCorsProcessor(new DefaultCorsProcessor() {
            @Override
            protected void rejectRequest(ServerHttpResponse response) throws IOException {
                if (response instanceof ServletServerHttpResponse servlet) {
                    securityErrorWriter.write(servlet.getServletResponse(), ErrorCode.FORBIDDEN);
                } else {
                    super.rejectRequest(response);
                }
            }
        });
        return httpSecurity.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                // 只接受 Authorization Bearer，未使用 Cookie、Session 或表单认证。
                .csrf(AbstractHttpConfigurer::disable).cors(AbstractHttpConfigurer::disable)
                .addFilter(corsFilter)
                .authorizeHttpRequests(
                        requests -> requests.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/",
                                        "/index.html",
                                        "/api/system/info",
                                        "/actuator/health"
                                )
                                .permitAll().requestMatchers("/api/**").hasRole("SERVICE")
                                .requestMatchers(HttpMethod.GET, "/actuator/metrics", "/actuator/metrics/**")
                                .hasRole("SERVICE")
                                .anyRequest().denyAll()
                )
                .exceptionHandling(
                        exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(
                        resource -> resource
                                .opaqueToken(opaque -> opaque.introspector(opaqueTokenIntrospector))
                                .authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler)
                )
                .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable).build();
    }

    /**
     * 固定服务凭据的最小实现，未来可替换为标准 token introspection。
     */
    @Bean
    @ConditionalOnMissingBean(OpaqueTokenIntrospector.class)
    OpaqueTokenIntrospector serviceTokenIntrospector(SecurityProperties securityProperties) {
        byte[] expectedTokenBytes = securityProperties.bearerToken() == null
                ? new byte[0]
                : securityProperties.bearerToken().getBytes(StandardCharsets.UTF_8);
        return token -> {
            if (!securityProperties.enabled()
                    || !MessageDigest.isEqual(expectedTokenBytes, token.getBytes(StandardCharsets.UTF_8))) {
                throw new BadOpaqueTokenException("Invalid service credential");
            }
            return new DefaultOAuth2AuthenticatedPrincipal(
                    "service",
                    Map.of("sub", "service"),
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
            );
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties securityProperties) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(securityProperties.allowedOrigins());
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type", "traceparent", "tracestate"));
        corsConfiguration.setExposedHeaders(List.of("X-Trace-Id", "X-Span-Id"));
        corsConfiguration.setAllowCredentials(false);
        corsConfiguration.setMaxAge(600L);
        UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        corsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return corsConfigurationSource;
    }

    /**
     * 抑制自动生成用户与密码；账号业务在后续设计。
     */
    @Bean
    InMemoryUserDetailsManager userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
