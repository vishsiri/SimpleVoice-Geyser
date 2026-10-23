---
title: MineDream Web Client Handoff
layout: projects
project: simplevoicegeyser
---

# MineDream Web Client Handoff

This fork accepts only the Webshop-hosted MineDream web client. Android `Svg-App`, legacy browser, username, and voice-password joins are rejected.

The client must obtain a short-lived ticket from the Webshop SSO flow and send exactly:

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

Do not send the Minecraft username, Webshop access token, password, or plugin API key through the WebSocket. The public Origin must exactly match `server.sso.allowed-origin`.

Test against the rebuilt plugin jar:

- a valid unused ticket for the online player's canonical UUID succeeds;
- replayed, expired, malformed, or mismatched-UUID tickets fail;
- missing or different Origin is rejected before the WebSocket opens;
- legacy username/password and `Svg-App` joins fail;
- capabilities and `capabilities_ack` still work after auth;
- chat still works;
- both audio directions work.
