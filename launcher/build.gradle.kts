// Thin wrapper launcher jar: self-attaches VelofineAgent, hands off to Minecraft's real main
// class, and doubles as the installer's --install-profile/--uninstall-profile CLI.
// Configuration inherited from the root project's `subprojects` block.
//
// Ships as a single shaded/fat jar, not a thin jar + separate dependency jars. Confirmed by a
// real launch test (Phase 1): dynamic self-attach (VirtualMachine.loadAgent) only puts the
// agent's own jar on the classpath, not its dependencies — a separate `core.jar` alongside it in
// .minecraft/libraries/ would need ProfileInstaller to know to copy it too, and silently break
// again the next time a class from a new dependency gets referenced unconditionally from
// VelofineAgent/Main. One self-contained jar removes that whole class of bug and matches
// Masterdoc's "thin wrapper launcher jar" (singular) language.

plugins {
    id("com.gradleup.shadow") version "9.6.1"
}

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

tasks.shadowJar {
    archiveClassifier.set("")
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

tasks.build {
    dependsOn(tasks.shadowJar)
}
