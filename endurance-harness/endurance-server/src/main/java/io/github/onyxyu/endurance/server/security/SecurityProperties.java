package io.github.onyxyu.endurance.server.security;

import jakarta.validation.constraints.AssertTrue;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("endurance.security")
public record SecurityProperties(boolean enabled, String bearerToken, List<String> allowedOrigins) {
    public SecurityProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }

    @AssertTrue(message = "enabled service authentication requires a token of 32 to 256 non-whitespace characters")
    public boolean isTokenConfigured() {
        return !enabled || (bearerToken != null && bearerToken.length() >= 32 && bearerToken.length() <= 256
                && bearerToken.chars().noneMatch(Character::isWhitespace));
    }

    @AssertTrue(message = "allowed-origins must contain explicit HTTP(S) origins without wildcard, path or query")
    public boolean isOriginsValid() {
        return allowedOrigins.stream().allMatch(origin -> {
            try {
                URI uri = URI.create(origin);
                return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                        && uri.getHost() != null && !origin.contains("*")
                        && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                        && uri.getRawQuery() == null && uri.getRawFragment() == null && uri.getUserInfo() == null;
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        });
    }

    @Override
    public String toString() {
        return "SecurityProperties[credentials=REDACTED]";
    }
}
