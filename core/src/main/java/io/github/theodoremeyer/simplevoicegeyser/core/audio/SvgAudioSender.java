package io.github.theodoremeyer.simplevoicegeyser.core.audio;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiosender.AudioSender;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import io.github.theodoremeyer.simplevoicegeyser.core.SvgCore;

import java.util.UUID;

/**
 * Audio Sender to send Clients Audio to server
 */
public final class SvgAudioSender {

    /** One 20 ms, 48 kHz, mono PCM16 frame from the web client. */
    public static final int PCM_FRAME_BYTES = 960 * Short.BYTES;

    /**
     * The simple voice chat api
     */
    private final VoicechatServerApi serverApi;
    /**
     * The player's uuid that the audio sender is associated with
     */
    private final UUID playerUuid;
    /**
     * The audio sender itself
     */
    private final AudioSender delegate;
    /**
     * Audio Encoder to encode the audio for svc
     */
    private final OpusEncoder encoder;
    private boolean closed;

    /**
     * Class Constructor. Creates and registers the audio sender
     * @param serverApi voice chat server api
     * @param playerUuid uuid of player registering sender for.
     */
    public SvgAudioSender(VoicechatServerApi serverApi, UUID playerUuid) {
        this.serverApi = serverApi;
        this.playerUuid = playerUuid;

        VoicechatConnection connection = serverApi.getConnectionOf(playerUuid);

        if (connection == null) {
            throw new RuntimeException("no svc connection for uuid: " + playerUuid);
        }

        OpusEncoder createdEncoder = serverApi.createEncoder();
        AudioSender createdDelegate = null;
        try {
            createdDelegate = serverApi.createAudioSender(connection);
            if (!serverApi.registerAudioSender(createdDelegate)) {
                throw new IllegalStateException("Failed to register audio sender for: " + playerUuid);
            }
            connection.setConnected(true);
            connection.setDisabled(false);
        } catch (RuntimeException exception) {
            closeEncoder(createdEncoder);
            if (createdDelegate != null) {
                try {
                    serverApi.unregisterAudioSender(createdDelegate);
                } catch (Exception ignored) {
                }
            }
            throw exception;
        }
        this.encoder = createdEncoder;
        this.delegate = createdDelegate;
    }

    /**
     * Sends Opus-encoded audio to the player if conditions are met.
     * @param pcmData data to send for player
     */
    public synchronized void sendOpus(byte[] pcmData) {
        if (closed || pcmData == null || pcmData.length != PCM_FRAME_BYTES) {
            return;
        }

        byte[] encoded;
        try {
            short[] pcmShorts = serverApi.getAudioConverter().bytesToShorts(pcmData);
            if (pcmShorts.length != 960) {
                SvgCore.getLogger().warning(
                        "[AudioSender] Invalid frame size: " + pcmShorts.length + " (expected 960)"
                );
                return;
            }

            encoded = encoder.encode(pcmShorts); // PCM to Opus encoded
            if (encoded == null || encoded.length == 0) {
                SvgCore.getLogger().warning("[AudioSender] Encoder returned empty data for: " + playerUuid);
                return;
            }
        } catch (Exception e) {
            SvgCore.getLogger().debug("AudioSender: Encoding failed for " + playerUuid, e);
            return;
        }

        boolean success = delegate.send(encoded);
        if (!success) {
            SvgCore.getLogger().warning("[AudioSender] Failed to send audio for: " + playerUuid);
        }
    }

    /**
     * Closes the AudioSender, and ends decoder
     */
    public synchronized void unregister() {
        if (closed) {
            return;
        }
        closed = true;

        try {
            encoder.resetState();
            encoder.close();
        } catch (Exception e) {
            SvgCore.getLogger().warning("[SvgAudioSender] Failed to close encoder for " + playerUuid + ": " + e.getMessage());
        }
        try {
            delegate.reset();
            serverApi.unregisterAudioSender(delegate);
        } catch (Exception e) {
            SvgCore.getLogger().debug("SvgAudioSender: Failed to unregister sender for " + playerUuid, e);
        }
    }

    private static void closeEncoder(OpusEncoder encoder) {
        try {
            encoder.resetState();
            encoder.close();
        } catch (Exception ignored) {
        }
    }
}
