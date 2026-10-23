package io.github.theodoremeyer.simplevoicegeyser.core.server.connection.auth;

import java.util.UUID;

/**
 * Identity returned by the trusted MineDream ticket-consume endpoint.
 * @param userId webshop user id
 * @param username current Minecraft username
 * @param minecraftUuid authoritative Minecraft UUID
 */
public record SsoIdentity(String userId, String username, UUID minecraftUuid) {
}
