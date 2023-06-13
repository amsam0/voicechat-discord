pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    }
}

rootProject.name = "voicechat-discord"
include("core", "paper", "fabric")
