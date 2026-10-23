package io.github.theodoremeyer.simplevoicegeyser.core.server;

import java.net.URI;

/**
 * Exact-origin gate for browser websocket upgrades.
 */
public final class OriginValidator {

    private OriginValidator() {
    }

    /**
     * Check an incoming Origin against the configured HTTPS origin.
     * @param origin incoming Origin header
     * @param allowedOrigin configured exact origin
     * @return true only for an exact match to a valid HTTPS origin
     */
    public static boolean allows(String origin, String allowedOrigin) {
        return isValidHttpsOrigin(allowedOrigin) && allowedOrigin.equals(origin);
    }

    static boolean isValidHttpsOrigin(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            return false;
        }

        try {
            URI uri = URI.create(value);
            String path = uri.getRawPath();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && (path == null || path.isEmpty());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
