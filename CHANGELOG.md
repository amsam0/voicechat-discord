# Simple Voice Chat Discord Bridge Changelog

This will mirror https://modrinth.com/plugin/simple-voice-chat-discord-bridge/changelog

## 2.0.0

Huge thanks to [Totobird](https://github.com/Totobird-Creations) for being a huge help with this update! Their PR was the main reason I started working on it again.

-   **All commands have been moved to subcommands on the `/dvc` command**
    -   See https://github.com/naturecodevoid/voicechat-discord#using-it-in-game for docs
    -   `/startdiscordvoicechat` was moved to `/dvc start`
        -   Running `/dvc start` while in a voice chat session restarts the session
    -   New subcommand: `/dvc stop`
        -   Only usable while currently in a discord voice chat session
        -   Disconnects the bot and stops the session
    -   New subcommand: `/dvc group`
        -   See https://github.com/naturecodevoid/voicechat-discord#dvc-group for docs
    -   New subcommand: `/dvc togglewhisper`
        -   Allows mod users to whisper
    -   New subcommand: `/dvc reloadconfig`
        -   Only usable by operators or players with the `voicechat-discord.reload-config` permission
        -   Stops all sessions and reloads the config
    -   New subcommand: `/dvc checkforupdate`
        -   Only usable by operators
        -   Checks for a new update using the GitHub API. If one is found, finds the version on Modrinth and links to the version page.
-   **Group support** (`/dvc group`)
    -   See https://github.com/naturecodevoid/voicechat-discord#dvc-group for docs
-   **Whispering support** (`/dvc togglewhisper`)
-   Added support for people using the mod to hear static/entity/locational audio channels
-   Use the new audio sender API to improve compatibility with other addons
-   [Fabric only] Use the Fabric Permissions API to support mods like LuckPerms for the reload config permission
-   Added version checker to ensure the plugin/mod is updated
-   Added Simple Voice Chat version checker to ensure we have a new enough version of the mod
-   Hopefully fixed [#5](https://github.com/naturecodevoid/voicechat-discord/issues/5)
-   Better login failure error handling and logging
-   Improvements to messages sent to players to be more clear
-   Optional debug logging to hopefully help with debugging issues
-   Major refactors and command handling improvements

Code changes: https://github.com/naturecodevoid/voicechat-discord/compare/1.4.0...2.0.0

## 1.4.0

This release should be functionally identical to 1.3.0 on fabric, but it fixed this paper specific bug: [(#4)](https://github.com/naturecodevoid/voicechat-discord/issues/4) On paper, the plugin
configuration folder is not created

Code changes: https://github.com/naturecodevoid/voicechat-discord/compare/1.3.0...1.4.0

## 1.3.0

-   Fixed [#2](https://github.com/naturecodevoid/voicechat-discord/issues/2)
-   Dropped Bukkit and Spigot support

Code changes: https://github.com/naturecodevoid/voicechat-discord/compare/1.2.0...1.3.0

## 1.2.0

> **Warning**
>
> This is the last release that supports spigot and bukkit. Later releases require Paper. If you are on Paper or Purpur, don't use this release, use the latest release. Only use this release if you
> are using Spigot/CraftBukkit and cannot use Paper or Purpur.

-   Fixed some issues with multiple bots ([#1](https://github.com/naturecodevoid/voicechat-discord/issues/1))
-   Fixed 2 users being able to start a voice chat with the same bot

Code changes: https://github.com/naturecodevoid/voicechat-discord/compare/1.1.0...1.2.0

## 1.1.0

-   Internal changes to support Bukkit and Fabric with the same codebase

Code changes: https://github.com/naturecodevoid/voicechat-discord/compare/1.0.0-build4...1.1.0

## 1.0.0

-   Initial release

Code: https://github.com/naturecodevoid/voicechat-discord/tree/1.0.0-build4
