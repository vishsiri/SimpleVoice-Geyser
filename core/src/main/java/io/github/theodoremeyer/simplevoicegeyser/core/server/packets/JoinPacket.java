package io.github.theodoremeyer.simplevoicegeyser.core.server.packets;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import io.github.theodoremeyer.simplevoicegeyser.core.SvgCore;
import io.github.theodoremeyer.simplevoicegeyser.core.server.connection.ConnectionStates;
import io.github.theodoremeyer.simplevoicegeyser.core.server.connection.SvgConnection;
import io.github.theodoremeyer.simplevoicegeyser.core.server.connection.auth.AuthException;
import io.github.theodoremeyer.simplevoicegeyser.core.server.connection.auth.AuthResponse;
import io.github.theodoremeyer.simplevoicegeyser.core.server.connection.auth.SsoIdentity;
import io.github.theodoremeyer.simplevoicegeyser.core.server.connection.compatibility.ClientCompatibilityResult;
import io.github.theodoremeyer.simplevoicegeyser.core.server.connection.compatibility.ClientCompatibilityValidator;
import io.github.theodoremeyer.simplevoicegeyser.core.server.connection.compatibility.ClientIdentity;
import io.github.theodoremeyer.simplevoicegeyser.core.server.servlets.JettyWebSocket;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

/**
 * Handles MineDream one-time ticket joins.
 */
public final class JoinPacket implements Packet {

    @Override
    public String getType() {
        return "join";
    }

    @Override
    public void handle(JettyWebSocket socket, JSONObject json) {
        SvgCore.getLogger().debug("WebSocket: Join attempt #"
                + socket.addJoinAttempt() + " from "
                + socket.getSession().getRemoteAddress());

        ClientCompatibilityResult compatibility = ClientCompatibilityValidator.validate(json);
        if (!compatibility.accepted()) {
            rejectCompatibility(socket, compatibility);
            return;
        }

        if (socket.getConnection() != null) {
            socket.getConnection().sendError("Already authenticated.", false);
            return;
        }

        Object rawTicket = json.opt("ticket");
        if (!(rawTicket instanceof String ticket) || ticket.isBlank()) {
            rejectAuthentication(socket);
            return;
        }

        if (!socket.beginAuthentication()) {
            socket.sendRaw(ConnectionStates.MessageType.ERROR, "Authentication already in progress.", false);
            return;
        }

        ClientIdentity clientIdentity = compatibility.identity();
        SvgCore.getSsoClient().consume(ticket).whenComplete((identity, error) -> {
            try {
                if (!isOpen(socket)) {
                    return;
                }

                if (error != null) {
                    SvgCore.getLogger().debug(
                            "WebSocket: SSO ticket consume failed type=" + rootCauseName(error)
                    );
                    rejectAuthentication(socket);
                    return;
                }

                finishJoin(socket, identity, clientIdentity);
            } catch (Exception exception) {
                SvgCore.getLogger().debug("WebSocket: Failed to finish SSO join", exception);
                rejectAuthentication(socket);
            } finally {
                socket.finishAuthentication();
            }
        });
    }

    private void finishJoin(JettyWebSocket socket, SsoIdentity identity, ClientIdentity clientIdentity) {
        if (socket.getConnection() != null) {
            socket.getConnection().sendError("Already authenticated.", false);
            return;
        }

        AuthResponse response = JettyWebSocket.AUTHENTICATOR.authenticate(identity);
        if (!response.success()) {
            rejectAuthentication(socket);
            return;
        }

        SvgConnection connection = SvgCore.getConnectionManager().connect(
                socket.getSession(),
                response.player(),
                socket.getAudioNegotiation(),
                clientIdentity
        );
        if (!socket.attachConnection(connection)) {
            SvgCore.getConnectionManager().remove(connection);
            connection.disconnect(1001, "WebSocket closed");
            return;
        }

        try {
            connection.authenticate();
        } catch (AuthException exception) {
            SvgCore.getLogger().debug("WebSocket: Failed to initialize voice connection", exception);
            SvgCore.getConnectionManager().remove(connection);
            connection.sendFatal(
                    "Failed to initialize voice chat.",
                    ConnectionStates.DisconnectCodes.FATAL_ERROR.getCode(),
                    "voice_init_failure"
            );
            return;
        }

        VoicechatConnection voicechatConnection =
                SvgCore.getBridge().getVcServerApi().getConnectionOf(response.uuid());

        if (SvgCore.getConfig().DEFAULT_GROUP_ENABLED.get() && voicechatConnection != null) {
            boolean forceDefaultGroup = SvgCore.getConfig().DEFAULT_GROUP_FORCE_ON_WEB_JOIN.get();
            boolean alreadyInGroup = voicechatConnection.isInGroup()
                    && voicechatConnection.getGroup() != null;

            if (!alreadyInGroup || forceDefaultGroup) {
                SvgCore.getGroupManager().createGroup(
                        response.player(),
                        "Svg",
                        SvgCore.getConfig().DEFAULT_GROUP_PASSWORD.get(),
                        Group.Type.OPEN,
                        false,
                        true
                );
            }
        }

        response.player().sendMessage(SvgCore.getPrefix() + "Connected!");
        connection.sendMessage(
                ConnectionStates.MessageType.STATUS,
                "Connected as " + connection.getPlayer().getName() + ".",
                false
        );
        SvgCore.getLogger().info("[WebSocket] "
                + connection.getPlayer().getName() + " authenticated with MineDream SSO.");
    }

    private void rejectCompatibility(JettyWebSocket socket, ClientCompatibilityResult compatibility) {
        SvgCore.getLogger().debug(
                "WebSocket: Join rejected by compatibility gate reason=" + compatibility.closeReason()
        );
        socket.sendRaw(ConnectionStates.MessageType.ERROR, compatibility.message(), false);
        close(socket, compatibility.closeCode(), compatibility.closeReason());
    }

    private void rejectAuthentication(JettyWebSocket socket) {
        socket.sendRaw(ConnectionStates.MessageType.ERROR, "Authentication failed.", true);
        close(
                socket,
                ConnectionStates.DisconnectCodes.PACKET_ERROR.getCode(),
                "authentication_failed"
        );
    }

    private void close(JettyWebSocket socket, int code, String reason) {
        Session session = socket.getSession();
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            session.close(code, reason);
        } catch (Exception ignored) {
        }
    }

    private boolean isOpen(JettyWebSocket socket) {
        return socket.isOpen();
    }

    private String rootCauseName(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName();
    }
}
