tasks.register<GradleBuild>("build") {
    group = "voicechat-discord"
    tasks = listOf(
            ":paper:build",
            ":fabric:build"
    )
}

tasks.register<GradleBuild>("publish") {
    group = "voicechat-discord"
    tasks = listOf(
            ":common:modrinthSyncBody",
            ":paper:modrinth",
            ":fabric:modrinth"
    )
}
