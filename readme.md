# Simple Voice Chat Discord Bridge

<!-- prettier-ignore-start -->
> **Warning**
>
> This is not an official addon. **Please don't go to the Simple Voice Chat discord server for support! Instead, please use [GitHub issues](https://github.com/naturecodevoid/voicechat-discord/issues)
> for support.** I'll try to provide support as soon as possible but there is no guarantee for how long it will take.
<!-- prettier-ignore-end -->

Simple Voice Chat Discord Bridge is a Fabric mod and Paper/Purpur plugin to make a bridge between Simple Voice Chat and Discord to allow for players without the mod to hear and speak. **This means
that Bedrock edition players connected through Geyser can use voice chat!**

## Installation and Usage

First, ensure that you have [Simple Voice Chat](https://modrinth.com/mod/simple-voice-chat) installed and correctly configured. Please refer to
[the Simple Voice Chat wiki](https://modrepo.de/minecraft/voicechat/wiki) for more info.

Then, you'll want to [download](https://modrinth.com/mod/simple-voice-chat-discord-bridge/versions) the latest version of Simple Voice Chat Discord Bridge that is compatible with your Minecraft
version.

<!-- prettier-ignore-start -->
> **Note**
>
> If you are using the Fabric mod, it requires the [Fabric API](https://modrinth.com/mod/fabric-api).
<!-- prettier-ignore-end -->

### Finding the configuration file

Make sure to run your server once with Simple Voice Chat and Simple Voice Chat Discord Bridge installed to generate Simple Voice Chat Discord Bridge's configuration file.

**Fabric:** Simple Voice Chat Discord Bridge's configuration file should be located at `config/voicechat-discord.yml`.

**Paper/Purpur:** Simple Voice Chat Discord Bridge's configuration file should be located at `plugins/voicechat-discord/config.yml`.

### Setting up a bot

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

<!-- prettier-ignore-start -->
> **Warning**
>
> There cannot be more than one person speaking in the voice channel at a time. **We recommend limiting bot voice channels to 2 users to ensure that this does not cause an issue.**
<!-- prettier-ignore-end -->

Now, reopen [the configuration file](#finding-the-configuration-file) with a text editor. Replace `VOICE_CHANNEL_ID_HERE` with the channel ID you copied. It should look something like this:

```yaml
bots:
    - token: TheTokenYouPasted
      vc_id: TheChannelIDYouPasted
```

You can redo this process for however many bots you want. There is some info in [the configuration file](#finding-the-configuration-file) about having multiple bots.

<!-- prettier-ignore-start -->
> **Note**
>
> The amount of bots you have is equivalent to the amount of people who can be connected to Simple Voice Chat through Discord at one time. So if you have 3 bots, up to 3 people can use the plugin at
> the same time.
<!-- prettier-ignore-end -->

### Using it in-game

<!-- prettier-ignore-start -->
> **Warning**
>
> There cannot be more than one person speaking in the voice channel at a time. **We recommend limiting bot voice channels to 2 users to ensure that this does not cause an issue.**
<!-- prettier-ignore-end -->

To use it in-game, use the `/dvc` command. (It may take a few seconds to start.) Then, join the voice channel as instructed. Other players should be able to hear you speak when you
speak in the Discord channel, and you should be able to hear other players speak when they speak through the Simple Voice Chat mod.
