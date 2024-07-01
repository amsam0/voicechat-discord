plugins {
    java
    id("com.modrinth.minotaur") version Properties.minotaurVersion
    id("com.github.johnrengelman.shadow") version Properties.shadowVersion
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

project.version = Properties.pluginVersion
project.group = Properties.mavenGroup

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(Properties.javaVersion))
}

tasks.compileJava {
    options.encoding = Charsets.UTF_8.name()

    options.release.set(Properties.javaVersion)
}

tasks.processResources {
    filteringCharset = Charsets.UTF_8.name()

    val properties = mapOf(
        "version" to Properties.pluginVersion,
        "paperApiVersion" to Properties.paperApiVersion,
    )
    inputs.properties(properties)

    filesMatching("plugin.yml") {
        expand(properties)
    }
}

tasks.runServer {
    minecraftVersion(Properties.paperMinecraftDevVersion)
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("shadow"))
    relocate("org.bspfsystems.yamlconfiguration", "dev.naturecodevoid.voicechatdiscord.shadow.yamlconfiguration")
    relocate("org.yaml.snakeyaml", "dev.naturecodevoid.voicechatdiscord.shadow.snakeyaml")
    relocate("com.github.zafarkhaja.semver", "dev.naturecodevoid.voicechatdiscord.shadow.semver")
    relocate("com.google.gson", "dev.naturecodevoid.voicechatdiscord.shadow.gson")
    relocate("net.kyori", "dev.naturecodevoid.voicechatdiscord.shadow.kyori")

    archiveBaseName.set(Properties.archivesBaseName + "-" + project.name)
    archiveClassifier.set("")
    archiveVersion.set(Properties.pluginVersion)
}

tasks.jar {
    archiveBaseName.set(Properties.archivesBaseName + "-" + project.name)
    archiveClassifier.set("")
    archiveVersion.set(Properties.pluginVersion)
}

tasks.reobfJar {
    // No idea why we didn't need to do this when we used Groovy, but this is necessary to have the correct jar filename (otherwise it will be paper-{VERSION}.jar)
    outputJar.set(layout.buildDirectory.file("libs/${Properties.archivesBaseName + "-" + project.name}-${Properties.pluginVersion}.jar"))
}

tasks.assemble {
    dependsOn(tasks.reobfJar.get())
}

tasks.build {
    dependsOn(tasks.shadowJar.get())
}

dependencies {
    paperweight.paperDevBundle(Properties.paperDevBundleVersion)

    compileOnly("de.maxhenkel.voicechat:voicechat-api:${Properties.voicechatApiVersion}")

    shadow("org.bspfsystems:yamlconfiguration:${Properties.yamlConfigurationVersion}")
    shadow("com.github.zafarkhaja:java-semver:${Properties.javaSemverVersion}")
    shadow("com.google.code.gson:gson:${Properties.gsonVersion}")
    // We need to be able to use the latest version of adventure (4.14.0), but Paper 1.19.4 uses 4.13.1
    // So we are forced to use the legacy platform implementation
    shadow("net.kyori:adventure-platform-bukkit:4.3.0")
    shadow("net.kyori:adventure-text-minimessage:${Properties.adventureVersion}")
    shadow("net.kyori:adventure-text-serializer-ansi:${Properties.adventureVersion}")
    shadow(project(":core"))
}

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://maven.maxhenkel.de/repository/public") }
    maven { url = uri("https://jitpack.io") }
    mavenLocal()
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(Properties.modrinthProjectId)
    versionName.set("[PAPER] " + project.version)
    versionNumber.set(Properties.pluginVersion)
    changelog.set("")
    uploadFile.set(tasks.reobfJar.get().outputJar.get())
    gameVersions.set(Properties.supportedMinecraftVersions)
    loaders.set(listOf("paper", "purpur"))
    detectLoaders.set(false)
    debugMode.set(System.getenv("MODRINTH_DEBUG") != null)
    dependencies {
        required.project("simple-voice-chat")
    }
}
