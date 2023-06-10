plugins {
    java
    id("com.modrinth.minotaur") version Properties.minotaurVersion
    id("com.github.johnrengelman.shadow") version Properties.shadowVersion
    id("fabric-loom") version "1.2-SNAPSHOT"
}

project.version = Properties.pluginVersion
project.group = Properties.mavenGroup

java {
    toolchain.languageVersion.set(Properties.javaLanguageVersion)
    sourceCompatibility = Properties.javaVersion
    targetCompatibility = Properties.javaVersion
}

tasks.compileJava {
    options.encoding = Charsets.UTF_8.name()

    options.release.set(Properties.javaVersionInt)
}

tasks.processResources {
    filteringCharset = Charsets.UTF_8.name()

    val properties = mapOf(
            "version" to Properties.pluginVersion,
            "fabricLoaderVersion" to Properties.fabricLoaderVersion,
            "minecraftVersion" to Properties.minecraftVersion,
            "voicechatApiVersion" to Properties.voicechatApiVersion,
            "javaVersion" to Properties.javaVersionInt.toString(),
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
    relocate("org.bspfsystems.yamlconfiguration", "dev.naturecodevoid.voicechatdiscord.shadow.yamlconfiguration")
    relocate("org.yaml.snakeyaml", "dev.naturecodevoid.voicechatdiscord.shadow.snakeyaml")
    relocate("com.github.zafarkhaja.semver", "dev.naturecodevoid.voicechatdiscord.shadow.semver")
    relocate("com.google.gson", "dev.naturecodevoid.voicechatdiscord.shadow.gson")
    relocate("net.dv8tion.jda", "dev.naturecodevoid.voicechatdiscord.shadow.jda")
    relocate("org.concentus", "dev.naturecodevoid.voicechatdiscord.shadow.concentus")

    archiveBaseName.set(Properties.archivesBaseName + "-fabric")
    archiveClassifier.set("")
    archiveVersion.set(Properties.pluginVersion)

    destinationDirectory.set(project.objects.directoryProperty().fileValue(file("${buildDir}/shadow")))
}

tasks.remapJar {
    archiveBaseName.set(Properties.archivesBaseName + "-fabric")
    archiveClassifier.set("")
    archiveVersion.set(Properties.pluginVersion)

    inputFile.set(tasks.shadowJar.get().archiveFile)
}

dependencies {
    minecraft("com.mojang:minecraft:${Properties.minecraftVersion}")
    mappings("net.fabricmc:yarn:${Properties.yarnMappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${Properties.fabricLoaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${Properties.fabricApiVersion}")

    modRuntimeOnly("maven.modrinth:simple-voice-chat:fabric-${Properties.minecraftVersion}-${Properties.voicechatApiVersion}")
    compileOnly("de.maxhenkel.voicechat:voicechat-api:${Properties.voicechatApiVersion}")

    implementation("org.bspfsystems:yamlconfiguration:${Properties.yamlConfigurationVersion}")
    shadow("org.bspfsystems:yamlconfiguration:${Properties.yamlConfigurationVersion}")

    implementation("com.github.zafarkhaja:java-semver:${Properties.javaSemverVersion}")
    shadow("com.github.zafarkhaja:java-semver:${Properties.javaSemverVersion}")

    implementation("com.google.code.gson:gson:${Properties.gsonVersion}")
    shadow("com.google.code.gson:gson:${Properties.gsonVersion}")

    implementation("com.github.naturecodevoid:JDA-concentus:${Properties.jdaConcentusVersion}")
    shadow("com.github.naturecodevoid:JDA-concentus:${Properties.jdaConcentusVersion}")

    implementation(project(":common"))
    shadow(project(":common"))
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
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
    maven { url = uri("https://maven.maxhenkel.de/repository/public") }
    mavenLocal()
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(Properties.modrinthProjectId)
    versionName.set("[FABRIC] " + project.version)
    versionNumber.set(Properties.pluginVersion)
    changelog.set("<a href=\"https://modrinth.com/mod/fabric-api\"><img alt=\"Requires Fabric API\" height=\"56\" src=\"https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/requires/fabric-api_vector.svg\" /></a>")
    uploadFile.set(tasks.remapJar)
    gameVersions.set(Properties.supportedMinecraftVersions)
    debugMode.set(System.getenv("MODRINTH_DEBUG") != null)
    dependencies {
        required.project("simple-voice-chat")
        required.project("fabric-api")
    }
}
