---
title: MineDream Voice Proxy
layout: projects
project: simplevoicegeyser
---

# MineDream Voice Proxy

The MineDream fork does not serve static web files and does not support the upstream subpath layout. `voicechat.minedream.city` is served by the Webshop monorepo; only `/ws` reaches the plugin.

## Context Path

The relevant config key is:

```yaml
server:
  context-path: /
```

Keep this `/` for the documented `/ws` route.

## Reverse Proxy Requirements

Configure the public host with two upstreams:

Required behavior:

| Public path | Upstream |
|-------------|----------|
| `/*` | Existing Webshop Web process |
| `/ws` | Private SimpleVoice-Geyser Jetty listener |

Enable WebSocket upgrades, keep the browser `Origin` header unchanged, and do not expose the Jetty port directly to the Internet. The plugin accepts only the exact HTTPS origin configured in `server.sso.allowed-origin`.

## Admin Checklist

- Keep `server.context-path: /`.
- Confirm websocket upgrade headers are forwarded.
- Confirm a different or missing Origin receives HTTP 403.
- Use `https://` and `wss://` publicly.
- Restrict Jetty ingress to the reverse proxy and restart the plugin after SSO config changes.
