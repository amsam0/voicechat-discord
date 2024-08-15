plugins {
    java
    id("com.modrinth.minotaur") version Properties.minotaurVersion
    id("com.github.johnrengelman.shadow") version Properties.shadowVersion
    id("fabric-loom") version "1.7-SNAPSHOT" // https://fabricmc.net/develop
}

project.version = Properties.pluginVersion
project.group = Properties.mavenGroup

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(Properties.javaVersion))
}

loom {
    runConfigs.configureEach {
        // Without this, none of the run configurations will be generated because this project is not the root project
        isIdeConfigGenerated = true
    }
}

tasks.compileJava {
    options.encoding = Charsets.UTF_8.name()

    options.release.set(Properties.javaVersion)
}

tasks.processResources {
    filteringCharset = Charsets.UTF_8.name()

    val properties = mapOf(
        "version" to Properties.pluginVersion,
        "fabricLoaderVersion" to Properties.fabricLoaderRequiredVersion,
        "minecraftVersion" to Properties.minecraftRequiredVersion,
        "voicechatApiVersion" to Properties.voicechatApiVersion,
        "javaVersion" to Properties.javaVersion.toString(),
    )
    inputs.properties(properties)

    filesMatching("fabric.mod.json") {
        expand(properties)
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${Properties.archivesBaseName}" }
    }
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("shadow"))
    relocate("org.bspfsystems.yamlconfiguration", "dev.amsam0.voicechatdiscord.shadow.yamlconfiguration")
    relocate("org.yaml.snakeyaml", "dev.amsam0.voicechatdiscord.shadow.snakeyaml")
    relocate("com.github.zafarkhaja.semver", "dev.amsam0.voicechatdiscord.shadow.semver")
    relocate("com.google.gson", "dev.amsam0.voicechatdiscord.shadow.gson")
    relocate("net.kyori", "dev.amsam0.voicechatdiscord.shadow.kyori")

    archiveBaseName.set(Properties.archivesBaseName + "-" + project.name)
    archiveClassifier.set("")
    archiveVersion.set(Properties.pluginVersion)

    destinationDirectory.set(project.objects.directoryProperty().fileValue(file("${buildDir}/shadow")))
}

tasks.remapJar {
    archiveBaseName.set(Properties.archivesBaseName + "-" + project.name)
    archiveClassifier.set("")
    archiveVersion.set(Properties.pluginVersion)

    inputFile.set(tasks.shadowJar.get().archiveFile)
}

dependencies {
    minecraft("com.mojang:minecraft:${Properties.fabricMinecraftDevVersion}")
    mappings("net.fabricmc:yarn:${Properties.yarnMappingsDevVersion}:v2")
    modImplementation("net.fabricmc:fabric-loader:${Properties.fabricLoaderDevVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${Properties.fabricApiDevVersion}")

    modImplementation("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")
    include("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")

    modRuntimeOnly("maven.modrinth:simple-voice-chat:fabric-${Properties.fabricMinecraftDevVersion}-${Properties.voicechatModRuntimeVersion}")
    compileOnly("de.maxhenkel.voicechat:voicechat-api:${Properties.voicechatApiVersion}")

    implementation("org.bspfsystems:yamlconfiguration:${Properties.yamlConfigurationVersion}")
    shadow("org.bspfsystems:yamlconfiguration:${Properties.yamlConfigurationVersion}")

    implementation("com.github.zafarkhaja:java-semver:${Properties.javaSemverVersion}")
    shadow("com.github.zafarkhaja:java-semver:${Properties.javaSemverVersion}")

    implementation("com.google.code.gson:gson:${Properties.gsonVersion}")
    shadow("com.google.code.gson:gson:${Properties.gsonVersion}")

    implementation("net.kyori:adventure-api:${Properties.adventureVersion}")
    implementation("net.kyori:adventure-text-minimessage:${Properties.adventureVersion}")
    implementation("net.kyori:adventure-text-serializer-ansi:${Properties.adventureVersion}")
    implementation("net.kyori:adventure-text-serializer-legacy:${Properties.adventureVersion}") // Fabric only
    shadow("net.kyori:adventure-api:${Properties.adventureVersion}")
    shadow("net.kyori:adventure-text-minimessage:${Properties.adventureVersion}")
    shadow("net.kyori:adventure-text-serializer-ansi:${Properties.adventureVersion}")
    shadow("net.kyori:adventure-text-serializer-legacy:${Properties.adventureVersion}") // Fabric only

    implementation(project(":core"))
    shadow(project(":core"))
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://maven.maxhenkel.de/repository/public") }
    mavenLocal()
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(Properties.modrinthProjectId)
    versionName.set("[FABRIC] " + project.version)
    versionNumber.set(Properties.pluginVersion)
    changelog.set("<a href=\"https://modrinth.com/mod/fabric-api\"><img alt=\"Requires Fabric API\" height=\"56\" src=\"https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/requires/fabric-api_vector.svg\" /></a>\n\n")
    uploadFile.set(tasks.remapJar)
    gameVersions.set(Properties.supportedMinecraftVersions)
    debugMode.set(System.getenv("MODRINTH_DEBUG") != null)
    dependencies {
        required.project("simple-voice-chat")
        required.project("fabric-api")
    }
}
