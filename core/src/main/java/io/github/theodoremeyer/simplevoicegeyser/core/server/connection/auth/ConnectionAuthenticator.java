package io.github.theodoremeyer.simplevoicegeyser.core.server.connection.auth;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import io.github.theodoremeyer.simplevoicegeyser.core.SvgCore;
import io.github.theodoremeyer.simplevoicegeyser.core.api.sender.SvgPlayer;
import io.github.theodoremeyer.simplevoicegeyser.core.geyser.GeyserHook;

import java.util.UUID;

/**
 * Validates an identity returned by MineDream SSO against live server state.
 */
public final class ConnectionAuthenticator {

    /**
     * Authenticate the authoritative SSO identity.
     * @param identity identity returned by the ticket-consume endpoint
     * @return authentication response
     */
    public AuthResponse authenticate(SsoIdentity identity) {
        try {
            if (identity == null) {
                return AuthResponse.failure("Authentication failed.");
            }

            UUID uuid = identity.minecraftUuid();
            SvgPlayer player = SvgCore.getPlayerManager().getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                return AuthResponse.failure("Authentication failed.");
            }

            AuthResponse bedrockResult = validateBedrock(uuid);
            if (!bedrockResult.success()) {
                return bedrockResult;
            }

            AuthResponse permissionResult = validatePermissions(player);
            if (!permissionResult.success()) {
                return permissionResult;
            }

            AuthResponse voiceChatResult = validateVoiceChat(uuid);
            if (!voiceChatResult.success()) {
                return voiceChatResult;
            }

            return AuthResponse.success(uuid, player);
        } catch (Exception exception) {
            SvgCore.getLogger().error("Authenticator: Unexpected SSO authentication failure", exception);
            return AuthResponse.failure("Authentication failed.");
        }
    }

    private AuthResponse validateBedrock(UUID uuid) {
        Boolean bedrock = GeyserHook.isBedrock(uuid);
        boolean requireBedrock = SvgCore.getConfig().REQUIRE_BEDROCK.get();

        if (bedrock == null) {
            if (requireBedrock) {
                SvgCore.getLogger().warning(
                        "Unable to enforce client.requireBedrock. Please install Floodgate or Geyser."
                );
            }
            return AuthResponse.ok();
        }

        if (!bedrock && requireBedrock) {
            return AuthResponse.failure("Authentication failed.");
        }
        return AuthResponse.ok();
    }

    private AuthResponse validatePermissions(SvgPlayer player) {
        if (!player.hasPermission("svg.vc.join")) {
            return AuthResponse.failure("Authentication failed.");
        }
        return AuthResponse.ok();
    }

    private AuthResponse validateVoiceChat(UUID uuid) {
        if (SvgCore.getBridge().getVcServerApi() == null) {
            SvgCore.getLogger().warning("[Authenticator] VoiceChatServerApi is null.");
            return AuthResponse.failure("Voice chat server unavailable.");
        }

        VoicechatConnection connection = SvgCore.getBridge().getVcServerApi().getConnectionOf(uuid);
        if (connection == null || connection.isInstalled()) {
            return AuthResponse.failure("Authentication failed.");
        }
        return AuthResponse.ok();
    }
}
