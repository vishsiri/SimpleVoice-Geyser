package io.github.theodoremeyer.simplevoicegeyser.core.server.connection.compatibility;

import io.github.theodoremeyer.simplevoicegeyser.core.server.connection.ConnectionStates;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientCompatibilityValidatorTest {

    @Test
    void acceptsOnlyTheMineDreamProtocolEnvelope() {
        ClientCompatibilityResult result = ClientCompatibilityValidator.validate(join(
                "MineDreamWeb", "1", "1"
        ));

        assertTrue(result.accepted());
        assertEquals(ClientIdentity.mineDreamWeb(), result.identity());
    }

    @Test
    void rejectsLegacyPasswordJoin() {
        ClientCompatibilityResult result = ClientCompatibilityValidator.validate(new JSONObject()
                .put("type", "join")
                .put("username", "PlayerName")
                .put("password", "secret"));

        assertFalse(result.accepted());
        assertEquals("invalid_client_info", result.closeReason());
    }

    @Test
    void rejectsUnknownClientType() {
        ClientCompatibilityResult result = ClientCompatibilityValidator.validate(join("Web", "1", "1"));

        assertFalse(result.accepted());
        assertEquals(ConnectionStates.DisconnectCodes.PACKET_ERROR.getCode(), result.closeCode());
        assertEquals("unsupported_client_type", result.closeReason());
    }

    @Test
    void rejectsWrongProtocolVersionOrBuild() {
        ClientCompatibilityResult wrongVersion = ClientCompatibilityValidator.validate(join(
                "MineDreamWeb", "2", "1"
        ));
        ClientCompatibilityResult wrongBuild = ClientCompatibilityValidator.validate(join(
                "MineDreamWeb", "1", "2"
        ));

        assertFalse(wrongVersion.accepted());
        assertFalse(wrongBuild.accepted());
        assertEquals(ConnectionStates.DisconnectCodes.OUTDATED_CLIENT.getCode(), wrongVersion.closeCode());
        assertEquals("update_required", wrongVersion.closeReason());
        assertEquals("update_required", wrongBuild.closeReason());
    }

    private static JSONObject join(String type, String version, String build) {
        return new JSONObject()
                .put("type", "join")
                .put("ticket", "opaque-ticket-value")
                .put("clientType", new JSONObject()
                        .put("type", type)
                        .put("serverVersion", version)
                        .put("serverBuild", build));
    }
}
