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
        return allowedOrigins.stream().allMatch(allowedOrigin -> {
            try {
                URI allowedOriginUri = URI.create(allowedOrigin);
                return ("http".equals(allowedOriginUri.getScheme()) || "https".equals(allowedOriginUri.getScheme()))
                        && allowedOriginUri.getHost() != null
                        && !allowedOrigin.contains("*")
                        && (allowedOriginUri.getRawPath() == null || allowedOriginUri.getRawPath().isEmpty())
                        && allowedOriginUri.getRawQuery() == null && allowedOriginUri.getRawFragment() == null
                        && allowedOriginUri.getUserInfo() == null;
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
