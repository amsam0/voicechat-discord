Project Status Update: https://github.com/naturecodevoid/voicechat-discord/issues/12

# Simple Voice Chat Discord Bridge

[<img alt="Modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg">](https://modrinth.com/plugin/simple-voice-chat-discord-bridge)
[<img alt="Requires Fabric API" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/requires/fabric-api_vector.svg">](https://modrinth.com/mod/fabric-api)

> **Warning**
>
> This is not an official addon. **Please don't go to the Simple Voice Chat discord server for support! Instead, please use [GitHub issues](https://github.com/naturecodevoid/voicechat-discord/issues)
> for support.** I'll try to provide support as soon as possible but there is no guarantee for how long it will take.

Simple Voice Chat Discord Bridge is a Fabric mod and Paper/Purpur plugin to make a bridge between Simple Voice Chat and Discord to allow for players without the mod to hear and speak. **This means
that Bedrock edition players connected through Geyser can use voice chat!**

Changelog: https://github.com/naturecodevoid/voicechat-discord/blob/master/CHANGELOG.md

# Installation and Usage

First, ensure that you have [Simple Voice Chat](https://modrinth.com/mod/simple-voice-chat) installed and correctly configured. Please refer to
[the Simple Voice Chat wiki](https://modrepo.de/minecraft/voicechat/wiki) for more info.

> **Note**
>
> Simple Voice Chat Discord Bridge requires version 2.4.11 or later of Simple Voice Chat. Please ensure you have updated!

Then, you'll want to [download](https://modrinth.com/mod/simple-voice-chat-discord-bridge/versions) the latest version of Simple Voice Chat Discord Bridge that is compatible with your Minecraft
version.

> **Note**
>
> If you are using the Fabric mod, it requires the [Fabric API](https://modrinth.com/mod/fabric-api).

## Finding the configuration file

Make sure to run your server once with Simple Voice Chat and Simple Voice Chat Discord Bridge installed to generate Simple Voice Chat Discord Bridge's configuration file.

**Fabric:** Simple Voice Chat Discord Bridge's configuration file should be located at `config/voicechat-discord.yml`.

**Paper/Purpur:** Simple Voice Chat Discord Bridge's configuration file should be located at `plugins/voicechat-discord/config.yml`.

## Setting up a bot

<sub>This guide is based off of and uses images from [DiscordSRV's Basic Setup guide](https://docs.discordsrv.com/installation/basic-setup/#setting-up-the-bot).</sub>

First, create an application at [discord.com/developers/applications](https://discord.com/developers/applications) by clicking `New Application`. Choose the name that you want your bot to be called.

![](https://docs.discordsrv.com/images/create_application.png)

On the left, click `Bot` and click `Add Bot` and confirm with `Yes, do it!`.

![](https://docs.discordsrv.com/images/create_bot.png)

Copy the token and disable `Public Bot`.

![](https://docs.discordsrv.com/images/copy_token.png)

Now, open [the configuration file](#finding-the-configuration-file) with a text editor. Replace `DISCORD_BOT_TOKEN_HERE` with the token you copied. It should look something like this:

```yaml
bots:
    - token: TheTokenYouPasted
      vc_id: VOICE_CHANNEL_ID_HERE
```

To invite the bot to a server, go to `General Information` and copy the Application ID.

![](https://docs.discordsrv.com/images/copy_application_id.png)

Go to
[discord.com/api/oauth2/authorize?client_id=YOUR_APPLICATION_ID_HERE&permissions=36700160&scope=bot](https://discord.com/api/oauth2/authorize?client_id=YOUR_APPLICATION_ID_HERE&permissions=36700160&scope=bot)
but replace `YOUR_APPLICATION_ID_HERE` with the application ID you just copied. Choose the server you want to invite your bot to. **Make sure not to disable any of its permissions.**

Now, follow the steps at [support.discord.com/articles/Where-can-I-find-my-User-Server-Message-ID](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-)
to enable Developer Mode in Discord. Then, right click the voice channel you want the bot to use as a bridge between Simple Voice Chat and Discord and click `Copy ID`.

> **Warning**
>
> There cannot be more than one person speaking in the voice channel at a time, or there will be weird audio glitches. **We recommend limiting bot voice channels to 2 users to ensure that this does
> not cause an issue.**

Now, reopen [the configuration file](#finding-the-configuration-file) with a text editor. Replace `VOICE_CHANNEL_ID_HERE` with the channel ID you copied. It should look something like this:

```yaml
bots:
    - token: TheTokenYouPasted
      vc_id: TheChannelIDYouPasted
```

You can redo this process for however many bots you want. There is some info in [the configuration file](#finding-the-configuration-file) about having multiple bots.

> **Note**
>
> The amount of bots you have is equivalent to the amount of people who can be connected to Simple Voice Chat through Discord at one time. So if you have 3 bots, up to 3 people can use the plugin at
> the same time.

## Using it in-game

> **Warning**
>
> There cannot be more than one person speaking in the voice channel at a time, or there will be weird audio glitches. **We recommend limiting bot voice channels to 2 users to ensure that this does
> not cause an issue.**

Most of Simple Voice Chat Discord Bridge's functionality is exposed through the `/dvc` command. This section will go through all of its subcommands.

For commands that take string arguments, you can wrap them in quotes to escape spaces.

### `/dvc start`

Starts a voice chat session using the first available bot. You may have to wait a few seconds for it to start. After it starts, join the voice channel as instructed. You should be able to hear players
speak, and other players should be able to hear you speak.

If you are having issues while in a voice chat session, you can try restarting it by using `/dvc start` again.

### `/dvc stop`

Stops the current voice chat session and disconnects the bot.

### `/dvc group`

Allows you to list, create, join, leave and remove groups.

#### `/dvc group list`

Gives you a list of all groups.

#### `/dvc group create <name> [password] [type] [persistent]`

Allows you to create a group.

Arguments:

-   `name` (required): The name of the group.
-   `password` (optional, defaults to `""` (no password)): The password of the group. If you don't want a password but want to change the group type or persistency, just pass an empty string: `""`
-   `type` (optional, defaults to `normal`): Can be `normal`, `open` or `isolated`.
    -   `normal`: Players in a group can hear nearby players that are not in a group, but not vice versa
    -   `open`: Players in a group can hear nearby players and nearby players can hear players in the group
    -   `isolated`: Players in a group can only hear other players in the group
-   `persistent` (optional, defaults to `false`): If `true`, the group will not be removed once all players leave. Instead, it has to be removed using [`/dvc group remove <ID>`](#dvc-group-remove-id)

#### `/dvc group join <ID>`

Allows you to join a group using an ID from [`/dvc group list`](#dvc-group-list).

#### `/dvc group info`

Gives you info about your current group.

#### `/dvc group leave`

Allows you to leave your current group.

#### `/dvc group remove <ID>`

Allows you to remove a **persistent** group **with no players in it** using an ID from [`/dvc group list`](#dvc-group-list).

### `/dvc togglewhisper`

Allows you to whisper.

### `/dvc reloadconfig`

If you are a operator (specifically permission level 2 or higher on fabric) or if you have the `voicechat-discord.reload-config` permission, you can use the `/dvc reloadconfig` subcommand to reload
the config without have to reload/restart the server. **Using this subcommand will stop all running bots.**

### `/dvc checkforupdate`

Checks for an update to the mod/plugin by first contacting GitHub's API, and then if a new version is found, using Modrinth's API to provide a link to that version's download page.

This is done automatically on server startup. By default, the update message (if there is an update) is sent to operators when they join the server. This can be disabled by disabling the
`alert_ops_of_updates` option in [the configuration file](#finding-the-configuration-file). However, this will not disable logging update messages to the console on startup.

## Roadmap

### Future

-   Catch config errors such as not in config when getting bots
-   Use a Rust discord library to hopefully permanently solve the SSL issues. I am unsure the feasibility of this since I heard that JNI is slow, so this may force us to use wasm or rewrite almost all of the mod in Rust (which is not a bad thing, but may cause it to be slower because of JNI). 
-   An option to lock the voice channels when they are not in use.
-   Reduce volume of crouching players in the audio that goes to discord. This is currently possible but not yet implemented
-   Action bar HUD for groups showing players in your group
-   Notifications for groups (a message is sent to you when someone joins/leaves the group you're in)
-   Inventory GUI for groups (this would need an abstraction between Bukkit's API and Fabric's sgui library, and would also make supporting 1.19.2+ on Fabric impossible)
