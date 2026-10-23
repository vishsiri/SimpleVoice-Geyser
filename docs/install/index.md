---
title: Installation
layout: projects
project: simplevoicegeyser
---

# Installation

This guide covers installing and configuring Simple Voice Geyser.

For Host-Specific setup, see [Hosting Guides]({% project_link guides %}).

---

## Requirements

### Bukkit-based Servers (Paper / Purpur / Spigot)

* Minecraft 1.21.8+
* Simple Voice Chat 2.6.x+
* GeyserMC 2.10.1-SNAPSHOT+
* Floodgate (optional, but recommended)

### Fabric

* Fabric Loader (matching your Minecraft version)
* Fabric API (required)
* Simple Voice Chat 2.6.x+
* GeyserMC 2.10.1-SNAPSHOT+
* LuckPerms (optional)
* Floodgate (optional)

> Simple Voice Geyser depends on Simple Voice Chat and will not function without it.

---

## Installation Steps

1. Download the latest release:
   https://github.com/TheodoreMeyer/SimpleVoice-Geyser/releases

2. Install the plugin/mod:

   * Bukkit: place the `.jar` in `plugins/`
   * Fabric: place the `.jar` in `mods/`

3. Install dependencies:

   * Simple Voice Chat
   * GeyserMC
   * Optional: Floodgate, LuckPerms (Fabric)

4. Start or restart your server

5. Verify startup:

   * No errors in console
   * Plugin loads successfully
   * Voice Chat is functioning

---

## Server Configuration

The plugin is configured via:
```yaml
client:

   # default: 30
   # Timeout in seconds
   # WARNING: Developer Has disabled this for right now.
   vctimeout: 30

   # default: 2
   # idle websocket timeout in minutes
   idletimeout: 2

   # default: false
   # Only allow bedrock players to join the chat
   requireBedrock: false

   join-message:

     #default: true
     # Enables join messages for web clients.
     enabled: true

     # Default: provided below
     # The text svg sends when a player joins
     text:
       - "This server uses MineDream Voice Chat."
       - "Open https://voicechat.minedream.city and sign in with your MineDream account."

   # default: true
   # Allows for the bedrock client to open a menu just by opening the emotes menu
   # WARNING: Make sure you have off-hand-emote turned off in geyser or there may
   #          be conflict
   useEmoteForSVG: true

  # default: true
  # Allows players using web-chat to chat in-game
   web-chat-enabled: true
   
updatechecker:
  enable: true

server:

   group:

      # Settings for the default group for Svg players.
      default:

         # default: true
         # This decides whether the Svg group is created for players who use the website
         enabled: true

         # default: 1a2b
         # This control's the default groups's password.
         password: 1a2b

   # default: 8080
   # port server will run on.
   port: 8080

   # default: 127.0.0.1
   # Keep Jetty private. Use a private interface only when the reverse proxy is on another host.
   bind-address: 127.0.0.1
   
   # default: /
   # Context path containing the websocket endpoint.
   # Example: /svg exposes the websocket at /svg/ws.
   context-path: /
   
   sso:
     api-base-url: "https://api.minedream.city"
     api-key: "replace-with-at-least-32-characters"
     allowed-origin: "https://voicechat.minedream.city"
     request-timeout-ms: 5000
   
   # See more at https://theodoremeyer.github.io/projects/simplevoicegeyser/install/audio-transport/
   audio:
     
     # default: svg-v2
     # options: legacy, svg-v2
     # This is the audio transport mode, deciding how to send audio to client.
     # legacy: uses the legacy audio transport mode, which is compatible with all clients.
     # svg-v2: uses the new svg-v2 audio transport mode, which is only compatible with clients that support it.
     # WARNING: This has been deprecated, and will be changed or removed in future releases.
     transport-mode: auto
     
     # default: true
     # Whether legacy mode is supported on your server.
     # If transport-mode is set to svg-v2, can legacy still be used.
     allow-legacy-fallback: true
     

# default: false
# Debug logs
# Do NOT use this in production. This will very quickly fill up your server logs.
debug: false
# default: 0.1.3
# Version of server associated with config
# DO NOT CHANGE, or config may be reset/messed up
config-version: 0.1.3
```

### Important Notes

* `vctimeout` is currently not enforced and may be removed or re-enabled later
* `useEmoteForSVG` may conflict with Geyser’s off-hand emote setting
* `bind-address` controls network exposure:

   * `127.0.0.1` → local reverse proxy only (recommended)
   * private interface IP → reverse proxy on the same private network
   * `0.0.0.0` → every interface; avoid unless the firewall limits ingress to the proxy

---

## Networking & Security

MineDream serves the browser client over HTTPS and proxies only WSS `/ws` to Jetty.

### Important:

* Browsers **require HTTPS** for microphone access in most environments
* Running on plain HTTP may result in:

   * microphone access being blocked
   * connection failures

### Recommendation

Use a reverse proxy (e.g., Nginx, Caddy) to provide:

* HTTPS (TLS)
* Secure WebSocket (WSS)

---

## Upgrading

Some versions may introduce breaking changes.

See: [Upgrading]({% project_link upgrading %})

---

## Versioning Notes

* Versions not be ready for production use may be marked as `-DEV`.

---

## Notes

* This plugin extends Simple Voice Chat; it does not replace it

* It does not provide voice chat on its own

* As of 0.1.2-DEV:

   * Audio Security
       * Client to server audio is not encoded
       * Server to client audio is only encoded in Svg-v2 audio protocol
   * The server runs over HTTP by default

This makes using HTTPS strongly recommended for real deployments.
