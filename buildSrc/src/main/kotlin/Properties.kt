import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

@Suppress("ConstPropertyName", "MemberVisibilityCanBePrivate")
object Properties {
    // "dev" versions refer to the development environment,
    // while "required" version refer to the version the mod says it needs
    // if not specified, assume "required"

    const val javaVersionRequiredInt = 17
    val javaVersionRequired = JavaVersion.VERSION_17
    val javaLanguageDevVersion = JavaLanguageVersion.of(21)

    const val minecraftRequiredVersion = "1.19.4"

    /* Paper */
    const val paperApiVersion = "1.19"
    const val paperMinecraftDevVersion = "1.21"
    const val paperDevBundleVersion = "1.21-R0.1-SNAPSHOT"

    /* Fabric (https://fabricmc.net/develop) */
    const val fabricLoaderRequiredVersion = "0.14.22"
    const val fabricMinecraftDevVersion = "1.21"
    const val yarnMappingsDevVersion = "$fabricMinecraftDevVersion+build.7"
    const val fabricLoaderDevVersion = "0.15.11"
    const val fabricApiDevVersion = "0.100.4+$fabricMinecraftDevVersion"

    /* Dependencies */
    const val voicechatApiVersion = "2.4.11"
    const val voicechatModRuntimeVersion = "2.5.16"
    const val yamlConfigurationVersion = "2.0.2"
    const val javaSemverVersion = "0.10.2"
    const val gsonVersion = "2.10.1"
    const val adventureVersion = "4.14.0"

    /* Project */
    const val pluginVersion = "3.0.0"
    const val mavenGroup = "dev.naturecodevoid.voicechatdiscord"
    const val archivesBaseName = "voicechat-discord"
    const val modrinthProjectId = "S1jG5YV5"
    val supportedMinecraftVersions = listOf("1.19.4", "1.20", "1.20.1", "1.20.2")

    /* Gradle Plugins */
    const val minotaurVersion = "2.+"
    const val shadowVersion = "8.1.1"
}
