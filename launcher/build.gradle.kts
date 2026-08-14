// Thin wrapper launcher jar: self-attaches VelofineAgent, hands off to Minecraft's real main
// class, and doubles as the installer's --install-profile/--uninstall-profile CLI.
// Configuration inherited from the root project's `subprojects` block.

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(project(":core"))
    implementation("com.google.code.gson:gson:2.14.0")

    // Mixin tooling decision (see CLAUDE.md "Mixin tooling decision"): SpongePowered Mixin,
    // chosen over plain ASM on SpongeVanilla's standalone-javaagent precedent. Dependency wired
    // now; a working custom IMixinService for our bare-agent (no ModLauncher/Fabric Knot)
    // environment is a substantial task deferred to the start of Phase 2. Unused in Phase 1.
    implementation("org.spongepowered:mixin:0.8.7")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "dev.velofine.launcher.Main",
            "Premain-Class" to "dev.velofine.launcher.VelofineAgent",
            "Agent-Class" to "dev.velofine.launcher.VelofineAgent",
            "Can-Retransform-Classes" to "true",
            "Can-Redefine-Classes" to "true"
        )
    }
}
