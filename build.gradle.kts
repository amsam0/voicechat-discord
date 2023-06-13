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
            ":core:modrinthSyncBody",
            ":paper:modrinth",
            ":fabric:modrinth"
    )
}
