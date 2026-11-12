package io.github.onyxyu.endurance.server.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 在 Spring HTTP Observation 之后、认证之前关联日志，不另造一套 trace ID。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceContextFilter extends OncePerRequestFilter {
    private final Tracer tracer;

    public TraceContextFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Span existing = tracer.currentSpan();
        Span span = existing == null ? tracer.nextSpan().name("http.inbound").start() : existing;
        long started = System.nanoTime();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span);
             MDC.MDCCloseable trace = MDC.putCloseable("traceId", span.context().traceId());
             MDC.MDCCloseable child = MDC.putCloseable("spanId", span.context().spanId())) {
            response.setHeader("X-Trace-Id", span.context().traceId());
            response.setHeader("X-Span-Id", span.context().spanId());
            try {
                chain.doFilter(request, response);
            } finally {
                // 不记录 query、Authorization、请求体或响应体。
                Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                log.info("HTTP {} route={} status={} durationMs={}", request.getMethod(),
                        route == null ? "unmatched" : route, response.getStatus(),
                        (System.nanoTime() - started) / 1_000_000);
            }
        } finally {
            if (existing == null) {
                span.end();
            }
        }
    }
}
