plugins {
    java
    id("com.modrinth.minotaur") version Properties.minotaurVersion
    id("com.github.johnrengelman.shadow") version Properties.shadowVersion
    id("io.papermc.paperweight.userdev") version "1.+"
    id("xyz.jpenilla.run-paper") version "2.0.0"
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
            "paperApiVersion" to Properties.paperApiVersion,
    )
    inputs.properties(properties)

    filesMatching("plugin.yml") {
        expand(properties)
    }
}

tasks.runServer {
    minecraftVersion(Properties.minecraftVersion)
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("shadow"))
    relocate("org.bspfsystems.yamlconfiguration", "dev.naturecodevoid.voicechatdiscord.shadow.yamlconfiguration")
    relocate("org.yaml.snakeyaml", "dev.naturecodevoid.voicechatdiscord.shadow.snakeyaml")
    relocate("com.github.zafarkhaja.semver", "dev.naturecodevoid.voicechatdiscord.shadow.semver")
    relocate("com.google.gson", "dev.naturecodevoid.voicechatdiscord.shadow.gson")
    relocate("net.dv8tion.jda", "dev.naturecodevoid.voicechatdiscord.shadow.jda")
    relocate("org.concentus", "dev.naturecodevoid.voicechatdiscord.shadow.concentus")

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
    shadow("com.github.naturecodevoid:JDA-concentus:${Properties.jdaConcentusVersion}")
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
    uploadFile.set(tasks.reobfJar)
    gameVersions.set(Properties.supportedMinecraftVersions)
    loaders.set(listOf("paper", "purpur"))
    detectLoaders.set(false)
    debugMode.set(System.getenv("MODRINTH_DEBUG") != null)
    dependencies {
        required.project("simple-voice-chat")
    }
}
