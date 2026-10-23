---
title: API
layout: projects
project: simplevoicegeyser
---
# API
This plugin/addon to SVG is not planning (currently) to have a large API. Instead, developers will be expected to connect over websocket to the server, via the server's configured address.

The MineDream fork accepts only short-lived, single-use Webshop tickets. The plugin validates the browser Origin, consumes the ticket through an authenticated HTTPS backchannel, and matches the returned UUID to an online player.

## Core
- You can build your own implementation of SimpleVoice-Geyser for a platform using the new Core Module.
- See the [Core Module](https://github.com/TheodoreMeyer/SimpleVoice-Geyser/tree/master/core) for more information.
- Download any jar with Core in it at [Releases](https://github.com/TheodoreMeyer/SimpleVoice-Geyser/releases).

## Websocket
- In Progress...
### Protocol
- Svg follows a protocol for server ↔ client communication.
- This can be found at [Protocol]({% project_link api/protocol %}).
- Only `MineDreamWeb` protocol version/build `1` is accepted by this fork.

# Notes:
- Feel free to come and give feedback or even contribute to this. I am open to ideas.
