package io.github.theodoremeyer.simplevoicegeyser.core.api.data;

import io.github.theodoremeyer.simplevoicegeyser.core.SvgCore;

import java.util.List;

/**
 * Represents the configuration for Simple Voice Geyser. This class is responsible for loading and saving the configuration file, as well as providing access to the configuration values.
 */
public final class SvgConfig {

    private final SvgFile file;

    /**
     * Create the Config with an associated config file
     * @param file file
     */
    public SvgConfig(SvgFile file) {
        this.file = file;
        applyDefaults();
    }

    /**
     * Get the underlying file
     * @return SvgFile
     */
    public SvgFile getFile() {
        if (file == null) {
            throw new IllegalStateException("SvgConfig not initialized");
        }
        return file;
    }

    private final ConfigKey<String> CONFIG_INFO =
            new ConfigKey <>(this, "config-info", "This file is used to configure Simple Voice Geyser. " +
                    "For more information, see the wiki: https://theodoremeyer.github.io/projects/simplevoicegeyser/");

    public final ConfigKey<Integer> VC_TIMEOUT =
            new ConfigKey <>(this, "client.vctimeout", 30);

    public final ConfigKey<Integer> IDLE_TIMEOUT =
            new ConfigKey <>(this, "client.idletimeout", 2);

    public final ConfigKey<Boolean> REQUIRE_BEDROCK =
            new ConfigKey <>(this, "client.requireBedrock", false);

    public final ConfigKey<Boolean> USE_EMOTE =
            new ConfigKey <>(this, "client.useEmoteForSVG", true);

    public final ConfigKey<Boolean> WEB_CHAT_ENABLED =
            new ConfigKey<>(this, "client.web-chat-enabled", true);

    public final ConfigKey<Boolean> JOIN_MESSAGE_ENABLED =
            new ConfigKey<>(this, "client.join-message.enabled", true);

    public final ConfigKey<List<String>> JOIN_MESSAGE_TEXT =
            new ConfigKey<>(this, "client.join-message.text", List.of(
                    "This server uses MineDream Voice Chat.",
                    "Open https://voicechat.minedream.city and sign in with your MineDream account."
            ));

    public final ConfigKey<Boolean> DEFAULT_GROUP_ENABLED =
            new ConfigKey <>(this, "server.group.default.enabled", true);

    public final ConfigKey<String> DEFAULT_GROUP_PASSWORD =
            new ConfigKey <>(this, "server.group.default.password", "1a2b");

    public final ConfigKey<Boolean> DEFAULT_GROUP_FORCE_ON_WEB_JOIN =
            new ConfigKey <>(this, "server.group.default.force-on-web-join", false);

    public final ConfigKey<Integer> PORT =
            new ConfigKey <>(this, "server.port", 8080);

    public final ConfigKey<String> BIND_ADDRESS =
            new ConfigKey <>(this, "server.bind-address", "127.0.0.1");

    public final ConfigKey<String> CONTEXT_PATH =
            new ConfigKey <>(this, "server.context-path", "/");

    public final ConfigKey<String> SSO_API_BASE_URL =
            new ConfigKey<>(this, "server.sso.api-base-url", "");

    public final ConfigKey<String> SSO_API_KEY =
            new ConfigKey<>(this, "server.sso.api-key", "");

    public final ConfigKey<String> SSO_ALLOWED_ORIGIN =
            new ConfigKey<>(this, "server.sso.allowed-origin", "https://voicechat.minedream.city");

    public final ConfigKey<Integer> SSO_REQUEST_TIMEOUT_MS =
            new ConfigKey<>(this, "server.sso.request-timeout-ms", 5000);

    public final ConfigKey<String> AUDIO_TRANSPORT_MODE =
            new ConfigKey <>(this, "server.audio.transport-mode", "svg-v2");

    public final ConfigKey<Boolean> AUDIO_ALLOW_LEGACY_FALLBACK =
            new ConfigKey <>(this, "server.audio.allow-legacy-fallback", true);

    public final ConfigKey<Boolean> DEBUG =
            new ConfigKey <>(this, "debug", false);

    public final ConfigKey<Boolean> UPDATE_CHECKER_ENABLED =
            new ConfigKey <>(this, "updatechecker.enable", true);

    public final ConfigKey<String> CONFIG_VERSION =
            new ConfigKey <>(this, "config-version", SvgCore.VERSION);

    private final List<ConfigKey<?>> ALL_KEYS = List.of(
            CONFIG_INFO,
            VC_TIMEOUT,
            IDLE_TIMEOUT,
            REQUIRE_BEDROCK,
            USE_EMOTE,
            WEB_CHAT_ENABLED,
            JOIN_MESSAGE_ENABLED,
            JOIN_MESSAGE_TEXT,
            DEFAULT_GROUP_ENABLED,
            DEFAULT_GROUP_PASSWORD,
            DEFAULT_GROUP_FORCE_ON_WEB_JOIN,
            PORT,
            BIND_ADDRESS,
            CONTEXT_PATH,
            SSO_API_BASE_URL,
            SSO_API_KEY,
            SSO_ALLOWED_ORIGIN,
            SSO_REQUEST_TIMEOUT_MS,
            AUDIO_TRANSPORT_MODE,
            AUDIO_ALLOW_LEGACY_FALLBACK,
            DEBUG,
            UPDATE_CHECKER_ENABLED,
            CONFIG_VERSION
    );

    /**
     * Apply Defaults to the file if any are missing
     */
    public void applyDefaults() {
        SvgFile file = getFile();

        String backupPath = null;
        int addedKeys = 0;
        boolean modified = false;

        // Add any missing config keys
        for (ConfigKey<?> key : ALL_KEYS) {
            if (!key.exists()) {

                if (!modified) {
                    backupPath = file.backup();
                    modified = true;
                }

                file.set(key.path(), key.def());
                addedKeys++;
            }
        }

        // Normalize context path if needed
        String currentContext = CONTEXT_PATH.get();
        String normalizedContext = normalizeContextPath(currentContext);
        if (!normalizedContext.equals(currentContext)) {

            if (!modified) {
                backupPath = file.backup();
                modified = true;
            }

            file.set(CONTEXT_PATH.path(), normalizedContext);
        }

        // Update config version if needed
        if (!SvgCore.VERSION.equals(CONFIG_VERSION.get())) {

            if (!modified) {
                backupPath = file.backup();
                modified = true;
            }

            file.set(CONFIG_VERSION.path(), SvgCore.VERSION);
        }

        if (modified) {
            file.save();

            SvgCore.getLogger().info(
                    "[Config] Configuration updated."
                            + (addedKeys > 0 ? " Added " + addedKeys + " missing key(s)." : "")
                            + (backupPath == null || backupPath.isBlank()
                            ? ""
                            : " Backup: " + backupPath)
            );
        }
    }

    /**
     * Take the ContextPath and try to normalize it
     * @param contextPath path to normalize
     * @return the normalized version
     */
    public static String normalizeContextPath(String contextPath) {
        if (contextPath == null) {
            return "/";
        }

        String normalized = contextPath.trim();
        if (normalized.isEmpty()) {
            return "/";
        }

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }
}
