// diagnostic.exe: a standalone remote-diagnostic tool for the v1.5 LegacySupport shader-rendering
// bug (see Masterdoc_v1.5.md / Build_plan_v1.5.md Phase 1). Intentionally decoupled from every
// other Velofine module - zero project(":...") dependencies - since it must run correctly on a
// machine that has never installed Velofine at all. Own version line, independent of Velofine's
// own release cycle (this script runs after the root `subprojects{}` block stamps the shared
// default, so this plain assignment wins).

import org.gradle.internal.os.OperatingSystem

version = "0.1.0"

plugins {
    id("com.gradleup.shadow") version "9.6.1"
}

// Single source of truth for "what version am I" (dev.velofine.diagnostics.ToolVersion), generated
// from project.version - mirrors core/build.gradle.kts's own BuildInfo templating, rather than a
// second hardcoded version literal living in Java source.
val toolVersionForResource = project.version.toString()

tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    inputs.property("toolVersion", toolVersionForResource)
    filesMatching("diagnostic-build-info.properties") {
        expand("toolVersion" to toolVersionForResource)
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")

    // Real, implementation-scope LWJGL - a deliberate divergence from legacysupport/mcstubs/shaders,
    // which only need compileOnly LWJGL for type references against Minecraft's own real LWJGL at
    // runtime. This module ships and runs standalone and is the first Velofine module to actually
    // create a live GL context, so it needs the real jars plus native libraries at runtime.
    implementation("org.lwjgl:lwjgl:3.4.1")
    implementation("org.lwjgl:lwjgl-glfw:3.4.1")
    implementation("org.lwjgl:lwjgl-opengl:3.4.1")
    runtimeOnly("org.lwjgl:lwjgl:3.4.1:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:3.4.1:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:3.4.1:natives-windows")
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes("Main-Class" to "dev.velofine.diagnostics.Main")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// --- Portable packaging: jpackage app-image + zip, mirroring installer/build.gradle.kts's proven
// pattern. "Portable single-file .exe, no install step" (Masterdoc_v1.5.md S5) can't be taken
// literally for a JVM app - jpackage --type app-image produces a folder (exe + app/ + runtime/),
// not one physical file. Resolved as: distribute that folder as one zip ("extract, double-click the
// exe inside, no wizard") rather than chasing a true single-file exe via GraalVM native-image, which
// has zero precedent in this repo and would need substantial reflect/resource config for LWJGL's
// dynamic native loading + Swing - too risky for a tool that must just work for a non-technical
// remote tester. See this-is-project-velofine-humming-milner.md S7 for the full rationale.

val appName = "VelofineDiagnostic"
val jpackageInputDir = layout.buildDirectory.dir("jpackage-input")
val jpackageOutputDir = layout.buildDirectory.dir("jpackage")
val distZipDir = layout.buildDirectory.dir("dist")

// Same numeric-only stripping trick as installer/build.gradle.kts's jpackageAppVersion - this
// module's own version is numeric-only today, but keep the same defensive regex so a future
// "0.2.0-rc1"-style bump doesn't silently break jpackage's strict --app-version parsing.
val jpackageAppVersion = Regex("^[0-9]+(\\.[0-9]+)*").find(project.version.toString())?.value ?: "0.1.0"

val stageJpackageInput = tasks.register<Sync>("stageJpackageInput") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar)
    into(jpackageInputDir)
}

val jpackageAppImage = tasks.register<Exec>("jpackageAppImage") {
    dependsOn(stageJpackageInput)
    inputs.dir(jpackageInputDir)
    outputs.dir(jpackageOutputDir)

    doFirst {
        delete(jpackageOutputDir)
    }

    val jpackageExe = if (OperatingSystem.current().isWindows) "jpackage.exe" else "jpackage"
    val mainJarName = (tasks.shadowJar.get() as Jar).archiveFileName.get()

    commandLine(
        "${System.getProperty("java.home")}/bin/$jpackageExe",
        "--type", "app-image",
        "--input", jpackageInputDir.get().asFile.absolutePath,
        "--main-jar", mainJarName,
        "--main-class", "dev.velofine.diagnostics.Main",
        "--name", appName,
        "--app-version", jpackageAppVersion,
        "--vendor", "siliconcode-dev",
        // Reuses the installer's existing branding art rather than duplicating it - diagnostics-tool
        // is a sibling module, not a dependency, so this is a plain relative-path reference.
        "--icon", rootProject.file("installer/branding/velofine.ico").absolutePath,
        "--dest", jpackageOutputDir.get().asFile.absolutePath
    )
}

val zipDistribution = tasks.register<Zip>("zipDistribution") {
    dependsOn(jpackageAppImage)
    archiveFileName.set("VelofineDiagnostic-${project.version}-win64.zip")
    destinationDirectory.set(distZipDir)
    from(jpackageOutputDir.get().dir(appName))
    into(appName)
}
