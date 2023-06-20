plugins {
    java
    id("com.modrinth.minotaur") version Properties.minotaurVersion
}

project.version = Properties.pluginVersion
project.group = Properties.mavenGroup

java {
    toolchain.languageVersion.set(Properties.javaLanguageVersion)
    sourceCompatibility = Properties.javaVersion
    targetCompatibility = Properties.javaVersion
}

tasks.jar {
    archiveBaseName.set(Properties.archivesBaseName + "-" + project.name)
}

tasks.register<Copy>("processSources") {
    filteringCharset = Charsets.UTF_8.name()

    val properties = mapOf(
            "version" to Properties.pluginVersion,
            "modrinthProjectId" to Properties.modrinthProjectId,
            "voicechatApiVersion" to Properties.voicechatApiVersion,
    )
    inputs.properties(properties)

    from("src/main/java") {
        include("**/Constants.java")

        expand(properties)
    }
    into("build/filteredSrc")
}

tasks.compileJava {
    options.encoding = Charsets.UTF_8.name()

    options.release.set(Properties.javaVersionInt)

    val javaSources = sourceSets["main"].allJava.filter {
        it.name != "Constants.java"
    }.asFileTree

    val processSources = tasks.getByName<Copy>("processSources")
    source = javaSources + fileTree(processSources.destinationDir)
    dependsOn(processSources)
}

dependencies {
    compileOnly("de.maxhenkel.voicechat:voicechat-api:${Properties.voicechatApiVersion}")

    compileOnly("org.bspfsystems:yamlconfiguration:${Properties.yamlConfigurationVersion}")
    compileOnly("com.github.zafarkhaja:java-semver:${Properties.javaSemverVersion}")
    compileOnly("com.google.code.gson:gson:${Properties.gsonVersion}")
    compileOnly("net.kyori:adventure-api:${Properties.adventureVersion}")
    compileOnly("net.kyori:adventure-text-minimessage:${Properties.adventureVersion}")
    compileOnly("net.kyori:adventure-text-serializer-ansi:${Properties.adventureVersion}")
    compileOnly("com.github.naturecodevoid:JDA-concentus:${Properties.jdaConcentusVersion}")
    compileOnly("com.mojang:brigadier:1.0.18")
}

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
    maven { url = uri("https://maven.maxhenkel.de/repository/public") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://libraries.minecraft.net") }
    mavenLocal()
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(Properties.modrinthProjectId)
    syncBodyFrom.set(rootProject.file("README.md").inputStream().bufferedReader().use { it.readText() })
    debugMode.set(System.getenv("MODRINTH_DEBUG") != null)
}
