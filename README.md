# MineDream Simple Voice Geyser

MineDream's private fork of [TheodoreMeyer/SimpleVoice-Geyser](https://github.com/TheodoreMeyer/SimpleVoice-Geyser).
It keeps the upstream audio bridge and replaces the browser username/password login with a single-use ticket issued by the MineDream Webshop.

The browser UI lives in the MineDream Webshop monorepo and is served from `https://voicechat.minedream.city`.
This plugin exposes only `/ws`; the bundled upstream web UI is not served.
[![GitHub release](https://img.shields.io/github/v/release/TheodoreMeyer/SimpleVoice-Geyser?include_prereleases)](https://github.com/TheodoreMeyer/SimpleVoice-Geyser/releases)
[![License](https://img.shields.io/github/license/TheodoreMeyer/SimpleVoice-Geyser)](https://github.com/TheodoreMeyer/SimpleVoice-Geyser/blob/master/LICENSE)
[Contributors](https://github.com/TheodoreMeyer/SimpleVoice-Geyser/blob/master/Contributors.md)

**Supports:**
- [![Spigot](https://img.shields.io/badge/Spigot-1.20.1–26.1.2-orange?logo=spigotmc&logoColor=white)](https://www.spigotmc.org/resources/132386)

- <a href="https://papermc.io/">
    <img src="https://assets.papermc.io/brand/papermc_logo.min.svg" height="22"/>
    <img src="https://img.shields.io/badge/Paper-1.20.1–26.1.2-3b82f6"/>
  </a>
- <a href="https://purpurmc.org/">
    <img src="https://purpurmc.org/docs/images/purpur-small.png" height="22"/>
    <img src="https://img.shields.io/badge/Purpur-1.20.1–26.1.2-7c3aed"/>
  </a>
- <a href="https://fabricmc.net/">
    <img src="https://fabricmc.net/assets/logo.png" height="22"/>
    <img src="https://img.shields.io/badge/Fabric-1.21.11–26.1.2-1f2937"/>
  </a>

A Geyser Extension to allow Bedrock Clients to connect with Simple voice Chat.

## MineDream authentication

The websocket accepts only this join envelope:

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

The plugin sends the ticket to the Webshop API over HTTPS with its dedicated `x-voice-api-key`.
The API response supplies the authoritative Minecraft UUID; the join succeeds only while that UUID is online and eligible for the voice bridge.
Legacy username/password joins are rejected.

See [MineDream SSO installation](docs/install/minedream-sso.md) and the [protocol reference](docs/api/protocol.md).

## What can the Plugin/extension do?
- Allow Bedrock Players to voice chat with Java Players through a web interface.
- Configured to also allow Java Players without the SVC Mod to join the chat.
- Configurable texting through a chatbox to allow the bedrock players to chat in-game.

## Using this Plugin
- Set the '.jar' file into your server's plugin directory.
- Configure Simple Voice Chat and the MineDream SSO backchannel. See [MineDream SSO installation](docs/install/minedream-sso.md).

### Updating
- Replacing the old jar with the new one.
     - I plan on making config options auto update.

## Documentation
- Fork documentation is available in [docs](docs/README.md).
- The [upstream wiki](https://theodoremeyer.github.io/projects/simplevoicegeyser/) remains useful for the audio bridge and group features only; its browser password setup does not apply to this fork.

- old wiki: [wiki](https://github.com/TheodoreMeyer/SimpleVoice-Geyser/wiki).

## Coming Soon
- Dev Release (v 0.1.4-Dev)
- Initial Release (v 1.0.0)

## Features to be worked on
- Support
  - Velocity
- More Web browser support.

## Suggestions?
Reach out through issues!

## Attribution and license

This fork remains under the upstream MIT license. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Dependencies used
- [Geysermc.org](https://geysermc.org)
- [Jetty.org](https://jetty.org)
- SimpleVoiceChat: [ModRepo.de](https://modrepo.de/minecraft/voicechat/overview)

## Important Notes
- Simple Voice Chat 2.6.0 or compatible is required to work, It must run on the server.
- GeyserMC 2.10.0-SNAPSHOT
- Java 25 or newer is required to build this project from source.

- Microphone/Speaker options may or may not work, depending on your browser, as this was built for Google Chrome.

## Developer Notes
- Please see the contributing.md before contributing to this project.
