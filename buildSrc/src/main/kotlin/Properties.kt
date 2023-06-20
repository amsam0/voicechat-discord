import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

@Suppress("ConstPropertyName", "MemberVisibilityCanBePrivate")
object Properties {
    const val javaVersionInt = 17
    val javaVersion = JavaVersion.VERSION_17
    val javaLanguageVersion = JavaLanguageVersion.of(javaVersionInt)

    const val minecraftVersion = "1.19.2"

    /* Paper */
    const val paperApiVersion = "1.19"
    const val paperDevBundleVersion = "$minecraftVersion-R0.1-SNAPSHOT"

    /* Fabric (https://fabricmc.net/develop/#latest-versions) */
    const val yarnMappings = "$minecraftVersion+build.28"
    const val fabricLoaderVersion = "0.14.10"
    const val fabricApiVersion = "0.76.0+$minecraftVersion"

    /* Dependencies */
    const val voicechatApiVersion = "2.4.11"
    const val yamlConfigurationVersion = "1.3.0"
    const val javaSemverVersion = "0.9.0"
    const val gsonVersion = "2.10.1"
    const val adventureVersion = "4.14.0"
    // NOTE: you may need to click "Get it" on jitpack.io for it to build
    const val jdaConcentusVersion = "a43689aa94"

    /* Project */
    const val pluginVersion = "2.0.1"
    const val mavenGroup = "dev.naturecodevoid.voicechatdiscord"
    const val archivesBaseName = "voicechat-discord"
    const val modrinthProjectId = "S1jG5YV5"
    val supportedMinecraftVersions = listOf("1.19.2", "1.19.3", "1.19.4", "1.20", "1.20.1")

    /* Gradle Plugins */
    const val minotaurVersion = "2.+"
    const val shadowVersion = "8.1.1"
}
