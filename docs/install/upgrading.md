---
title: Upgrading
layout: projects
project: simplevoicegeyser
---

# Upgrading to the MineDream fork

Back up the plugin directory, replace the JAR, and configure `server.sso` before starting.
The fork does not read or delete the old player credential file; legacy username/password clients stop working immediately.

The Webshop client and plugin must both use `MineDreamWeb` protocol version/build `1`.
Restart the plugin after changing any SSO setting, then verify Origin rejection, one-time ticket consumption, UUID matching, chat, and both audio directions before production rollout.
