import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

@Suppress("ConstPropertyName", "MemberVisibilityCanBePrivate")
object Properties {
    const val javaVersionInt = 17
    val javaVersion = JavaVersion.VERSION_17
    val javaLanguageVersion = JavaLanguageVersion.of(javaVersionInt)

    const val minecraftVersion = "1.19.4"

    /* Paper */
    const val paperApiVersion = "1.19"
    const val paperDevBundleVersion = "$minecraftVersion-R0.1-SNAPSHOT"

    /* Fabric (https://fabricmc.net/develop/#latest-versions) */
    const val yarnMappings = "$minecraftVersion+build.2"
    const val fabricLoaderVersion = "0.14.22"
    const val fabricApiVersion = "0.87.0+$minecraftVersion"

    /* Dependencies */
    const val voicechatApiVersion = "2.4.11"
    const val yamlConfigurationVersion = "1.3.3"
    const val javaSemverVersion = "0.9.0"
    const val gsonVersion = "2.10.1"
    const val adventureVersion = "4.14.0"
    // NOTE: you may need to click "Get it" on jitpack.io for it to build
    const val jdaConcentusVersion = "f9021a872a"

    /* Project */
    const val pluginVersion = "2.1.0"
    const val mavenGroup = "dev.naturecodevoid.voicechatdiscord"
    const val archivesBaseName = "voicechat-discord"
    const val modrinthProjectId = "S1jG5YV5"
    val supportedMinecraftVersions = listOf("1.19.4", "1.20", "1.20.1", "1.20.2")

    /* Gradle Plugins */
    const val minotaurVersion = "2.+"
    const val shadowVersion = "8.1.1"
}
