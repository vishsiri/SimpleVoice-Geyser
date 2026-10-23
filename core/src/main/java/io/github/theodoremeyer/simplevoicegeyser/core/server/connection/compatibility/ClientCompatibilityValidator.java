package io.github.theodoremeyer.simplevoicegeyser.core.server.connection.compatibility;

import io.github.theodoremeyer.simplevoicegeyser.core.server.connection.ConnectionStates;
import org.json.JSONObject;

/**
 * Validates the fixed MineDream web protocol envelope.
 */
public final class ClientCompatibilityValidator {

    private static final String CLIENT_TYPE = "MineDreamWeb";
    private static final String PROTOCOL_VERSION = "1";
    private static final String PROTOCOL_BUILD = "1";

    private static final ClientCompatibilityResult INVALID_CLIENT_INFO =
            ClientCompatibilityResult.rejected(
                    "Invalid client information.",
                    ConnectionStates.DisconnectCodes.PACKET_ERROR.getCode(),
                    "invalid_client_info"
            );

    private ClientCompatibilityValidator() {
    }

    /**
     * Validate a MineDream join envelope. Legacy clients are deliberately rejected.
     * @param join join packet
     * @return compatibility result
     */
    public static ClientCompatibilityResult validate(JSONObject join) {
        Object rawClientType = join.opt("clientType");
        if (!(rawClientType instanceof JSONObject clientType)) {
            return INVALID_CLIENT_INFO;
        }

        if (!CLIENT_TYPE.equals(exactString(clientType, "type"))) {
            return ClientCompatibilityResult.rejected(
                    "Unsupported client type.",
                    ConnectionStates.DisconnectCodes.PACKET_ERROR.getCode(),
                    "unsupported_client_type"
            );
        }

        String serverVersion = exactString(clientType, "serverVersion");
        String serverBuild = exactString(clientType, "serverBuild");
        if (serverVersion == null || serverBuild == null) {
            return INVALID_CLIENT_INFO;
        }

        if (!PROTOCOL_VERSION.equals(serverVersion) || !PROTOCOL_BUILD.equals(serverBuild)) {
            return ClientCompatibilityResult.rejected(
                    "This client is not compatible with this server.",
                    ConnectionStates.DisconnectCodes.OUTDATED_CLIENT.getCode(),
                    "update_required"
            );
        }

        return ClientCompatibilityResult.accepted(ClientIdentity.mineDreamWeb());
    }

    private static String exactString(JSONObject json, String key) {
        Object value = json.opt(key);
        if (!(value instanceof String text) || text.isBlank() || !text.equals(text.trim())) {
            return null;
        }
        return text;
    }
}
