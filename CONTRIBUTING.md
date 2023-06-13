# Contributing to Simple Voice Chat Discord Bridge

First of all, thank you for your interest in contributing to Simple Voice Chat Discord Bridge! I don't have much motivation to work on this project anymore, so any contributions are greatly
appreciated.

This document will not go over any of the following topics:

-   Basics of Java
-   Minecraft modding/plugin development

## `buildSrc` and `Properties`

In `buildSrc`, there is a Kotlin object named `Properties`. It has constants of various properties and replaced `gradle.properties` when I migrated to using Kotlin for gradle build scripts.

Please try to extract versions and similar things to this object if it's used more than once.

## Code Style

If you do not use IntelliJ IDEA, leave the code formatting to me. Also, please don't add any unnecessary whitespace.

If you want to separate larger classes like `DiscordBot` into smaller classes, please contact me before doing so. I don't like OOP and usually, you will need to give me a good reason why you want to
do this.

Try to make as much as possible `private` and `final`. If something is not `private`, it should be `public`.

Feel free to write more comments and improve this guide if you think something needs explaining. However, I don't think everything needs to be commented, most things such as variables should be self
explanatory based on their name. If it's not clear, try thinking of a better name.

## Code Structure

The project is split into 3 sub projects:

-   `core`: The core of the project. As much as possible of the mod/plugin is implemented here, to reduce duplicate code between Paper and Fabric.
-   `paper` and `fabric`: The Paper plugin and Fabric mod, providing implementations of [`Platform`](#platform-supporting-both-paper-and-fabric) to allow `core` to handle almost everything.

### `DiscordBot`: The core of the audio transfer system

The main part of the audio transfer system is in `DiscordBot`. It does the following:

-   `login()` and `start()`: Handles logging into the Discord bot, starting the voice connection with Discord and creating the SVC audio listener & audio sender.
-   `stop()`: Handles cleaning up everything related to the bot, providing a clean slate if the bot is reused.
-   Handles outgoing (going to Discord) audio.
    -   `handleOutgoingSoundPacket(packet)`: Handles packets received by the SVC audio listener.
    -   `pollOutgoingAudio()`: Returns 20ms of unencoded audio that should go to Discord.
-   Handles incoming (going to SVC) audio.
    -   `handleEncodedAudio(packet)`: Handles packets received by Discord. These are sent directly to the SVC audio sender.

An `AudioSource` is an source of audio that's going to Discord. These are identified by UUID. Each `AudioSource` has a dedicated queue of outgoing audio and opus decoder.

The `resetWatcher` thread handles resetting the audio sender, discord encoder and decoders for all audio sources. It checks every 100ms if it has been 100ms since they were last used, and if so, it
resets them.

### `Core`: Shared events between Paper and Fabric and manager of things

The `Core` class does a few things:

-   Has all the required event handlers
    -   `enable()`, `disable()`, `onPlayerJoin(player)`, `onPlayerLeave(player)`
    -   `paper` and `fabric` both register events for these functions to be called.
-   Manages all the bots and provides functions for getting a bot.
-   Manages the config.
-   Checks if the SVC version is new enough.

Most of the `public` functions are called by `paper` and `fabric`.

### `Platform`: Supporting both Paper and Fabric

The `Platform` interface has many methods that allow an abstraction between the Paper and Fabric APIs. Both `paper` and `fabric` have implementations of it.

It's not very pretty, as `Object` is used a lot, so if you have suggestions on how to improve it, please tell me.

### `SubCommands`: The `/dvc` command

The `SubCommands` has a single public static method that's used to build the `/dvc` command using Brigadier. Most commands should be wrapped using `wrapInTry()`.

All commands should be extracted to private methods, and larger commands such as `/dvc group` should have their own class.

### Other stuff

`UpdateChecker` is run in a separate thread on `enable()` and uses the GitHub and Modrinth API to check for a new update and get the Modrinth version page.

`VoicechatPlugin` is the plugin for the voicechat API. It sets `Core.api` and registers event handlers.

`GroupManager` keeps track of groups and has the source code of the event handles used to do so.

`Constants` has `static final` constants, usually `String`s, that are used elsewhere. These can be replaced with properties at compile time. See `core/build.gradle.kts` if you want to add more
properties.

`AudioCore` has various algorithms and utilities for modifying and combining audio.
