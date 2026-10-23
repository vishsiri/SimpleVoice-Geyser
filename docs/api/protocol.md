---
title: Websocket Protocol
layout: projects
project: simplevoicegeyser
---

# Protocol Reference

This page documents the MineDream web client and plugin packet flow, not the internal Simple Voice Chat protocol. Legacy browser and native Android authentication are intentionally unsupported.

## Connection

The browser connects to the fixed websocket endpoint on the Voice Chat origin:

```text
wss://voicechat.minedream.city/ws
```

The Webshop serves every normal page and asset request. Only `/ws` is routed to the plugin's private Jetty listener. Public deployments require HTTPS/WSS for microphone access and transport security.

## Message Types

The protocol uses two websocket message classes:

| Direction        | Message class | Purpose                                                   |
|------------------|---------------|-----------------------------------------------------------|
| Client to server | JSON text     | Join, capabilities, chat                                  |
| Server to client | JSON text     | Status, errors, chat, capabilities acknowledgement        |
| Client to server | Binary        | Microphone audio                                          |
| Server to client | Binary        | Receive audio, either legacy PCM or `svg-v2` framed audio |

## MineDream SSO join

The external Webshop client obtains an opaque, short-lived, single-use ticket. It sends no username, password, Webshop access token, refresh token, or API key to the websocket.

```json
{
  "type": "join",
  "ticket": "<single-use-ticket>",
  "clientType": {
    "type": "MineDreamWeb",
    "serverVersion": "1",
    "serverBuild": "1"
  }
}
```

`clientType`, `serverVersion`, and `serverBuild` are exact, fixed protocol values. There is no legacy browser, username/password, or `Svg-App` fallback in this fork.

Validation order:

| Condition | Result |
|-----------|--------|
| WebSocket Origin does not exactly match the configured HTTPS origin | HTTP 403 before websocket creation. |
| Missing or malformed `clientType` | Close `4005` / `invalid_client_info`. |
| Client type is not `MineDreamWeb` | Close `4005` / `unsupported_client_type`. |
| Protocol version/build is not `1` | Close `4008` / `update_required`. |
| Ticket is missing or malformed | Close `4005` / `authentication_failed`. |
| Ticket API times out, returns non-2xx, or returns malformed identity data | Close `4005` / `authentication_failed`. |
| Returned UUID is not currently online or cannot use the bridge | Close `4005` / `authentication_failed`. |
| Successful authentication | Create the voice bridge connection and send a status message. |

The plugin consumes the ticket asynchronously through `POST /api/voice-sso/tickets/consume` with its server-only `x-voice-api-key`. Tickets, API keys, response bodies, and complete join packets must never be logged.

Successful join response:

```json
{
  "type": "status",
  "message": "Connected as PlayerName.",
  "fatal": false
}
```

After this status message, the current browser client sends its audio capability packet.

## Capability Packet

The capability packet lets the server choose `legacy` or `svg-v2` per websocket session.

Client to server:

```json
{
  "type": "capabilities",
  "audio": {
    "protocols": ["legacy", "svg-v2"],
    "supportsOpusDecoder": true,
    "secureContext": true,
    "decoder": {
      "opusWasm": true,
      "webCodecs": false,
      "wasmError": null,
      "webCodecsError": "disabled_by_policy_wasm_only"
    }
  }
}
```

Notes:

| Field                       | Meaning                                                                               |
|-----------------------------|---------------------------------------------------------------------------------------|
| `audio.protocols`           | Supported receive-audio transports. Unsupported/degraded browsers send only `legacy`. |
| `audio.supportsOpusDecoder` | Whether the client can decode Opus for `svg-v2` receive audio.                        |
| `audio.secureContext`       | Browser secure-context state.                                                         |
| `audio.decoder.opusWasm`    | Whether the browser WASM Opus decoder is ready.                                       |
| `audio.decoder.webCodecs`   | Reserved for future app/non-browser clients. The current browser path is WASM-only.   |

Server acknowledgement:

```json
{
  "type": "capabilities_ack",
  "selectedMode": "svg-v2",
  "fallbackCount": 0
}
```

`selectedMode` is either `legacy` or `svg-v2`.

If `server.audio.transport-mode` is `auto`, the server selects `svg-v2` only when the client reports compatible protocol and decoder support. Otherwise, it uses `legacy` when `server.audio.allow-legacy-fallback` is enabled.

## Chat Packet

Client to server:

```json
{
  "type": "chat",
  "message": "Hello from web chat"
}
```

Server behavior:

| Condition                                    | Result                                                                       |
|----------------------------------------------|------------------------------------------------------------------------------|
| Client is not authenticated                  | Server sends an error.                                                       |
| Message contains unsupported characters only | Server sends an error.                                                       |
| Message is valid                             | Server forwards sanitized chat and echoes a chat response to the web client. |

Server chat response:

```json
{
  "type": "chat",
  "message": "You: Hello from web chat",
  "fatal": false
}
```

## Server Status And Error Packets

Server text packets use this shape:

```json
{
  "type": "status",
  "message": "Human-readable message",
  "fatal": false
}
```

Supported text packet `type` values:

| Type               | Meaning                                   |
|--------------------|-------------------------------------------|
| `status`           | Normal state update.                      |
| `error`            | Recoverable or fatal error message.       |
| `chat`             | Chat message delivered to the web client. |
| `generic`          | Non-specific informational message.       |
| `capabilities_ack` | Audio transport selection response.       |

When `fatal` is `true`, the browser stops reconnecting automatically.

## Close Codes

|   Code | Meaning                                 |
|-------:|-----------------------------------------|
| `1001` | Generic close.                          |
| `4001` | Session replaced by another connection. |
| `4002` | Timeout close.                          |
| `4003` | Player left the game.                   |
| `4004` | Fatal error.                            |
| `4005` | Packet error.                           |
| `4006` | Server shutdown.                        |
| `4007` | Closed session.                         |
| `4008` | Outdated client.                        |

Known close reasons used by compatibility validation:

| Code   | Reason                       | Meaning                                                   |
|--------|------------------------------|-----------------------------------------------------------|
| `4005` | `invalid_client_info`        | Client type metadata is malformed or incomplete.          |
| `4005` | `unsupported_client_type`    | The client type is not `MineDreamWeb`.                    |
| `4005` | `authentication_failed`      | Ticket or live-player validation failed.                  |
| `4008` | `update_required`            | MineDream protocol version/build is unsupported.          |

## Client To Server Audio

The MineDream web client sends microphone audio as binary websocket frames after authentication.

Current packet shape:

| Field              | Value                           |
|--------------------|---------------------------------|
| Encoding           | PCM signed 16-bit little-endian |
| Channels           | Mono                            |
| Sample rate        | 48000 Hz                        |
| Samples per packet | 960                             |
| Bytes per packet   | 1920                            |

The server validates the packet as 960 PCM samples, encodes it to Opus through the Simple Voice Chat API, and forwards it into the voice chat connection.

Client mic Opus encoding is intentionally split into a future PR. It is not part of the 0.1.2 PR #45 protocol changes.

## Server To Client Legacy Audio

In `legacy` mode, server-to-client audio is a raw binary PCM frame without an envelope.

Packet shape:

| Field           | Value                                             |
|-----------------|---------------------------------------------------|
| Encoding        | PCM signed 16-bit little-endian                   |
| Channels        | Usually stereo after server-side spatial handling |
| Sample rate     | 48000 Hz                                          |
| Envelope/header | None                                              |

The browser treats non-`svg-v2` binary frames as legacy PCM.

## Server To Client `svg-v2` Audio

In `svg-v2` mode, server-to-client audio uses a versioned binary frame. Multibyte fields are little-endian.

| Field        |     Size | Description                                |
|--------------|---------:|--------------------------------------------|
| `magic`      |  2 bytes | ASCII `SV`                                 |
| `version`    |   1 byte | `2`                                        |
| `flags`      |   1 byte | Packet flags                               |
| `sequence`   |  4 bytes | Packet sequence number                     |
| `panQ15`     |  2 bytes | Signed Q15 pan metadata, `-1.0` to `1.0`   |
| `gainQ15`    |  2 bytes | Unsigned Q15 gain metadata, `0.0` to `1.0` |
| `sampleRate` |  2 bytes | Sample rate, normally `48000`              |
| `channels`   |   1 byte | Source channel count                       |
| `codec`      |   1 byte | `1=opus`, `2=pcm16le`                      |
| `payloadLen` |  4 bytes | Payload length                             |
| `payload`    | variable | Codec payload                              |

Known flag values:

| Flag                       |  Value | Meaning                                          |
|----------------------------|-------:|--------------------------------------------------|
| `FLAG_STATIC_PACKET`       | `0x01` | Source packet is static/non-positional.          |
| `FLAG_DISTANCE_ATTENUATED` | `0x02` | Server calculated distance attenuation metadata. |
| `FLAG_HAS_PAN`             | `0x04` | Frame includes pan metadata.                     |

The current `svg-v2` receive path usually sends Opus payloads with compact spatial metadata. The browser decodes the payload and applies pan/gain locally before enqueueing PCM into the speaker worklet.

## Compatibility Rules

| Client/server state                                            | Expected behavior                               |
|----------------------------------------------------------------|-------------------------------------------------|
| New client, `transport-mode=auto`, decoder ready               | Server selects `svg-v2`.                        |
| New client, decoder unavailable                                | Server uses `legacy` when fallback is enabled.  |
| Old client with no capability packet                           | Server remains on `legacy`.                     |
| `transport-mode=legacy`                                        | Server always uses `legacy`.                    |
| `transport-mode=svg-v2`, fallback disabled, unsupported client | No compatible receive-audio path is guaranteed. |

## Local Manual Testing Checklist

- Exact `https://voicechat.minedream.city` Origin upgrades; missing or different Origin receives HTTP 403.
- `MineDreamWeb` protocol version/build `1` reaches ticket consumption.
- Legacy username/password, `Web`, and `Svg-App` joins are rejected before ticket consumption.
- Ticket timeout, non-2xx, reuse, malformed UUID, and offline UUID all close with generic `authentication_failed`.
- A valid ticket for an online UUID joins and cannot be reused.
- Authenticated MineDream web clients send `capabilities` and receive `capabilities_ack`.
- Chat and microphone audio continue to work after authentication.
- Server-to-client legacy and `svg-v2` audio behavior remain unchanged.
