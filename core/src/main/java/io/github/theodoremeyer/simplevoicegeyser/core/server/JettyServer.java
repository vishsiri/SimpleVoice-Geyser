package io.github.theodoremeyer.simplevoicegeyser.core.server;

import io.github.theodoremeyer.simplevoicegeyser.core.SvgCore;
import io.github.theodoremeyer.simplevoicegeyser.core.audio.SvgAudioSender;
import io.github.theodoremeyer.simplevoicegeyser.core.server.servlets.JettyWebSocket;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.io.IOException;
import java.time.Duration;

/**
 * Starts and Stops the Jetty server for SVG.
 * May be moved to a new thread.
 */
public final class JettyServer {

    private static final int MAX_CONTROL_MESSAGE_SIZE = 16 * 1024;

    /**
     * The Server
     */
    private final Server server;

    /**
     * Idle Timeout
     */
    private final Duration idleTimeout;

    /**
     * set server port
     * @param host the host address to bind the server to.
     * @param port port to run server on
     */
    public JettyServer(String host, int port) {
        this.server = new Server();

        double idleTimeoutMinutes = SvgCore.getConfig().IDLE_TIMEOUT.get();

        idleTimeoutMinutes = Math.clamp(idleTimeoutMinutes, 0.5, 10.0);

        SvgCore.getLogger().info("Idle timeout: " + idleTimeoutMinutes + " minutes.");

        this.idleTimeout = Duration.ofSeconds(
                Math.round(idleTimeoutMinutes * 60)
        );



        ServerConnector connector = new ServerConnector(server);
        connector.setHost(host);
        connector.setPort(port);
        connector.setIdleTimeout(idleTimeout.toMillis());

        server.addConnector(connector);
        SvgCore.getLogger().info("Started on: " + connector.getDefaultProtocol() + " " + connector.getHost() + ":" + connector.getPort());
    }

    /**
     * starts Jetty server
     * @throws Exception start server error
     */
    public void start() throws Exception {
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath(SvgCore.getConfig().CONTEXT_PATH.get());
        server.setHandler(context);

        // Register WebSocket at /ws
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.addMapping("/ws", (req, resp) -> {
                String allowedOrigin = SvgCore.getConfig().SSO_ALLOWED_ORIGIN.get();
                if (!OriginValidator.allows(req.getOrigin(), allowedOrigin)) {
                    try {
                        resp.sendForbidden("Forbidden");
                    } catch (IOException exception) {
                        SvgCore.getLogger().debug("WebSocket: Failed to reject invalid Origin", exception);
                    }
                    return null;
                }
                return new JettyWebSocket();
            });
            wsContainer.setIdleTimeout(idleTimeout);
            wsContainer.setMaxTextMessageSize(MAX_CONTROL_MESSAGE_SIZE);
            wsContainer.setMaxBinaryMessageSize(SvgAudioSender.PCM_FRAME_BYTES);
        });

        server.start();
    }

    /**
     * Stops Jetty Server
     * @throws Exception stop error
     */
    public void stop() throws Exception {
        server.stop();
    }
}
