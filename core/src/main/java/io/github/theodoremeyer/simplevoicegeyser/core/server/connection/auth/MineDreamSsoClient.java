package io.github.theodoremeyer.simplevoicegeyser.core.server.connection.auth;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Consumes single-use MineDream voice tickets over the API backchannel.
 */
public final class MineDreamSsoClient {

    private static final String CONSUME_PATH = "/api/voice-sso/tickets/consume";
    private static final int MAX_RESPONSE_BYTES = 8192;
    private static final int MAX_PENDING_REQUESTS = 64;

    private final HttpClient httpClient;
    private final URI consumeUri;
    private final String apiKey;
    private final Duration timeout;
    private final PendingTicketRegistry pendingTickets = new PendingTicketRegistry(MAX_PENDING_REQUESTS);

    /**
     * Create the fail-closed SSO client.
     * @param apiBaseUrl HTTPS API origin, without a path
     * @param apiKey dedicated voice API key
     * @param timeoutMs request and connection timeout in milliseconds
     */
    public MineDreamSsoClient(String apiBaseUrl, String apiKey, int timeoutMs) {
        URI baseUri = validateApiBaseUrl(apiBaseUrl);
        this.apiKey = validateApiKey(apiKey);

        if (timeoutMs < 500 || timeoutMs > 30_000) {
            throw new IllegalArgumentException("server.sso.request-timeout-ms must be between 500 and 30000");
        }

        this.timeout = Duration.ofMillis(timeoutMs);
        this.consumeUri = baseUri.resolve(CONSUME_PATH);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Consume a ticket without blocking the websocket or server thread.
     * @param ticket opaque single-use ticket
     * @return authenticated identity
     */
    public CompletableFuture<SsoIdentity> consume(String ticket) {
        if (!isTicketShapeValid(ticket)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid ticket"));
        }
        if (!pendingTickets.tryStart(ticket)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Ticket consume unavailable"));
        }

        try {
            String requestBody = new JSONObject().put("ticket", ticket).toString();
            HttpRequest request = HttpRequest.newBuilder(consumeUri)
                    .timeout(timeout)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("x-voice-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            throw new IllegalStateException("Ticket consume rejected");
                        }
                        return parseIdentity(response.body());
                    })
                    .whenComplete((identity, error) -> pendingTickets.finish(ticket));
        } catch (RuntimeException exception) {
            pendingTickets.finish(ticket);
            return CompletableFuture.failedFuture(exception);
        }
    }

    static boolean isTicketShapeValid(String ticket) {
        if (ticket == null || ticket.length() != 43) {
            return false;
        }

        for (int i = 0; i < ticket.length(); i++) {
            char ch = ticket.charAt(i);
            if (!(ch >= 'A' && ch <= 'Z')
                    && !(ch >= 'a' && ch <= 'z')
                    && !(ch >= '0' && ch <= '9')
                    && ch != '_'
                    && ch != '-') {
                return false;
            }
        }
        return true;
    }

    static SsoIdentity parseIdentity(String body) {
        if (body == null
                || body.length() > MAX_RESPONSE_BYTES
                || body.getBytes(StandardCharsets.UTF_8).length > MAX_RESPONSE_BYTES) {
            throw new IllegalArgumentException("Invalid identity response");
        }

        JSONObject json = new JSONObject(body);
        String userId = requireString(json, "userId", 128);
        String username = requireString(json, "username", 64);
        String uuidText = requireString(json, "minecraftUuid", 36);

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid identity response", exception);
        }

        if (!uuid.toString().equalsIgnoreCase(uuidText)) {
            throw new IllegalArgumentException("Invalid identity response");
        }

        return new SsoIdentity(userId, username, uuid);
    }

    private static String requireString(JSONObject json, String key, int maxLength) {
        Object value = json.opt(key);
        if (!(value instanceof String text)
                || text.isBlank()
                || text.length() > maxLength
                || !text.equals(text.trim())) {
            throw new IllegalArgumentException("Invalid identity response");
        }

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isISOControl(ch)) {
                throw new IllegalArgumentException("Invalid identity response");
            }
        }
        return text;
    }

    private static URI validateApiBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("server.sso.api-base-url is required");
        }

        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("server.sso.api-base-url must be a valid HTTPS origin", exception);
        }

        String path = uri.getRawPath();
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || (path != null && !path.isEmpty() && !"/".equals(path))) {
            throw new IllegalArgumentException("server.sso.api-base-url must be an HTTPS origin");
        }
        return uri;
    }

    private static String validateApiKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("server.sso.api-key is required");
        }

        String key = value.trim();
        if (key.length() < 32 || key.length() > 512 || !key.equals(value)) {
            throw new IllegalArgumentException("server.sso.api-key must be 32 to 512 non-whitespace characters");
        }

        for (int i = 0; i < key.length(); i++) {
            char ch = key.charAt(i);
            if (ch < 0x21 || ch > 0x7E) {
                throw new IllegalArgumentException("server.sso.api-key contains invalid characters");
            }
        }
        return key;
    }
}
