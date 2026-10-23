---
title: Security
layout: projects
project: simplevoicegeyser
---

# Security

The MineDream fork has no player voice password and does not serve the browser UI.

- Only the exact configured HTTPS Origin may upgrade `/ws`.
- Only `MineDreamWeb` protocol version/build `1` is accepted.
- A 43-character base64url ticket is consumed once through the HTTPS Webshop API backchannel.
- The dedicated `x-voice-api-key` remains on the server and is never sent to browsers or logged.
- The authoritative UUID returned by the API must belong to a currently online player.
- API failures and identity failures use the same generic websocket close reason.

Keep the Jetty port private behind the reverse proxy, rotate the dedicated API key if exposed, and use `wss://` publicly. See [MineDream SSO]({% project_link install/minedream-sso %}) for configuration.
