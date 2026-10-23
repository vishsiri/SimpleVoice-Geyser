---
title: MineDream SSO
layout: projects
project: simplevoicegeyser
---

# MineDream SSO installation

The MineDream fork does not serve a browser UI and does not accept voice passwords.
Route `https://voicechat.minedream.city/ws` to this plugin and route every other path to the Webshop web app.

## Plugin configuration

The first start adds these keys to the normal SVG configuration:

```yaml
server:
  bind-address: "127.0.0.1"
  sso:
    api-base-url: "https://api.minedream.city"
    api-key: "replace-with-a-dedicated-voice-api-key"
    allowed-origin: "https://voicechat.minedream.city"
    request-timeout-ms: 5000
```

- `api-base-url` must be an HTTPS origin without a path, query, credentials, or fragment.
- `api-key` must be a dedicated key of at least 32 characters matching the Webshop API configuration. Never expose it to the browser.
- `allowed-origin` is an exact HTTPS Origin. A missing, different, `http://`, or trailing-slash value is rejected.
- `request-timeout-ms` must be between 500 and 30000 milliseconds.
- Keep `bind-address` on loopback when the reverse proxy is local; otherwise use only the private interface reachable by that proxy.
- SSO settings are loaded at startup; restart the plugin after changing them.

The plugin refuses to start when the API URL/key is missing or insecure. It never follows API redirects.

## Reverse proxy

Forward the browser's `Origin` header unchanged and support WebSocket upgrades on `/ws`.
Terminate public TLS at the proxy, and keep the plugin's Jetty port private to the proxy/server network.

The plugin calls:

```text
POST https://api.minedream.city/api/voice-sso/tickets/consume
content-type: application/json
x-voice-api-key: <dedicated secret>

{"ticket":"<single-use-ticket>"}
```

A successful 2xx response must contain only the identity data the plugin consumes:

```json
{
  "userId": "<webshop-user-id>",
  "username": "PlayerName",
  "minecraftUuid": "0f8fad5b-d9cb-469f-a165-70867728950e"
}
```

Tickets are exactly 43 base64url characters (`A-Z`, `a-z`, `0-9`, `_`, `-`). The UUID must be canonical and currently online. Non-2xx, timeout, malformed JSON, missing fields, or an offline UUID closes the websocket with the generic `authentication_failed` reason.
