package io.github.theodoremeyer.simplevoicegeyser.core.server.connection.auth;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MineDreamSsoClientTest {

    @Test
    void parsesOnlyACompleteCanonicalIdentity() {
        UUID uuid = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
        SsoIdentity identity = MineDreamSsoClient.parseIdentity(new JSONObject()
                .put("userId", "user-123")
                .put("username", "PlayerName")
                .put("minecraftUuid", uuid.toString())
                .toString());

        assertEquals("user-123", identity.userId());
        assertEquals("PlayerName", identity.username());
        assertEquals(uuid, identity.minecraftUuid());
    }

    @Test
    void rejectsMissingOrMalformedIdentityFields() {
        assertThrows(IllegalArgumentException.class, () -> MineDreamSsoClient.parseIdentity("{}"));
        assertThrows(IllegalArgumentException.class, () -> MineDreamSsoClient.parseIdentity(new JSONObject()
                .put("userId", "user-123")
                .put("username", "PlayerName")
                .put("minecraftUuid", "not-a-uuid")
                .toString()));
    }

    @Test
    void rejectsTicketsWithWhitespaceOrControlCharacters() {
        assertTrue(MineDreamSsoClient.isTicketShapeValid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        assertFalse(MineDreamSsoClient.isTicketShapeValid("short-ticket"));
        assertFalse(MineDreamSsoClient.isTicketShapeValid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa+"));
    }

    @Test
    void rejectsInsecureApiUrlsAndShortApiKeys() {
        assertThrows(IllegalArgumentException.class, () -> new MineDreamSsoClient(
                "http://api.minedream.city",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                5000
        ));
        assertThrows(IllegalArgumentException.class, () -> new MineDreamSsoClient(
                "https://api.minedream.city",
                "too-short",
                5000
        ));
    }
}
