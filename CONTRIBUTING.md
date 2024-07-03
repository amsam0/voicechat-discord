# Contributing to Simple Voice Chat Discord Bridge

First of all, thank you for your interest in contributing to Simple Voice Chat Discord Bridge! I don't have much motivation to work on this project anymore, so any contributions are greatly
appreciated.

This document will not go over any of the following topics:

-   Basics of Java
-   Minecraft modding/plugin development

## Using `net.minecraft` classes on Paper

Since this is a pretty important topic, I've chosen to put this section at the top. This is only relevant for the Paper side of things, since Fabric has intermediary mappings which solves the whole
problem this section goes over.

On Paper, we cannot easily use methods on classes in the `net.minecraft` package. This is because we want to target 1.19.4+; obfuscated names **will** change between versions, and paperweight will
only reobfuscate the jar for 1.19.4.

Class names are mostly fine as those seem to be unobfuscated by Paper; methods are the main problem.

Try to use Bukkit methods and reflection to use methods that won't be obfuscated. For example, instead of using `net.minecraft.world.entity.Entity.getX()`, use the `getBukkitSender` method to get the
Bukkit `Entity` (`getBukkitEntity` also exists, but I had issues with it) and then use the Bukkit API to get the X coordinate. This avoids using the `net.minecraft` method and instead uses a method
(`getBukkitSender`) that Bukkit will implement and therefore won't be obfuscated.

For methods such as `net.minecraft.server.MinecraftServer.getCommands()` that don't have a Bukkit counterpart, you may be able to use some clever reflection to get the method name through return type
and arguments. See `DvcBrigadierCommand` for examples of this.

Brigadier classes are completely fine to use. However, instead of using `net.minecraft`'s `CommandSourceStack`, use Paper's `com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource` when
casting as it should have all the methods you will need.

Keep in mind that classes in the `org.bukkit.craftbukkit` package, such as everything in `org.bukkit.craftbukkit.<version>.command.VanillaCommandWrapper`, can be used safely with the original method
name through reflection. See `BukkitHelper`'s `get...` methods and `DvcBrigadierCommand.getListener(sender)` for examples of this.

## Native Rust library

As of 3.0.0, voicechat-discord uses JNI and a rust library to communicate with Discord. You will need the following dependencies to build it:

-   Rust and cargo
-   `libopus`
-   `pkg-config`

I recommend installing rust with rustup. As for `libopus`, you can install it with your system's package manager. If you use nix, a `shell.nix` file is provided in the `core` directory.

The source code of the rust library is in `core/src/main/rust`. To build the rust library, simply run `cargo build` in `core`. To copy the build library into the correct directory so that it is loaded
by the addon, run the `copy_natives.sh` script. (If you made a release build, run `copy_natives.sh release`. Otherwise it will copy the debug binary.)

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

-   `core`: The core of the project. As much as possible of the mod/plugin is implemented here, to reduce duplicate code between Paper and Fabric. This sub project also includes the native rust
    library.
-   `paper` and `fabric`: The Paper plugin and Fabric mod, providing implementations of [`Platform`](#platform-supporting-both-paper-and-fabric) to allow `core` to handle almost everything.

Note: some of this is a bit outdated. I updated some of it for 3.0.0 but there may be some incorrect information.

### `DiscordBot`: The core of the audio transfer system

The main part of the audio transfer system is in `DiscordBot`. It does the following:

-   `login()` and `start()`: Handles logging into the Discord bot, starting the voice connection with Discord and creating the SVC audio listener & audio sender.
-   `stop()`: Handles cleaning up everything related to the bot, providing a clean slate if the bot is reused.
-   Handles audio going from SVC to Discord and from Discord to SVC. Much of this is done on the Rust side; various functions are called on the Java side to orchestrate everything.

The `resetWatcher` thread handles resetting the audio sender and decoders for all audio sources. It checks every 100ms if it has been 100ms since they were last used, and if so, it resets them.

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
