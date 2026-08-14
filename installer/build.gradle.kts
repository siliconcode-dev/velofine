// jpackage (app-image, bundles Java 25) -> Inno Setup packaging pipeline for the Velofine
// installer. Non-Java module: no src/main/java, java-library from the root `subprojects` block
// just produces an empty jar here, which is harmless and unused.

import org.gradle.internal.os.OperatingSystem

val appName = "Velofine"
val jpackageInputDir = layout.buildDirectory.dir("jpackage-input")
val jpackageOutputDir = layout.buildDirectory.dir("jpackage")
val innoOutputDir = layout.buildDirectory.dir("innosetup")

val launcherJar = project(":launcher").tasks.named("jar")
val launcherRuntimeClasspath = project(":launcher").configurations.named("runtimeClasspath")

val stageJpackageInput = tasks.register<Sync>("stageJpackageInput") {
    dependsOn(launcherJar)
    from(launcherJar)
    from(launcherRuntimeClasspath)
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
    val mainJarName = (launcherJar.get() as Jar).archiveFileName.get()

    commandLine(
        "${System.getProperty("java.home")}/bin/$jpackageExe",
        "--type", "app-image",
        "--input", jpackageInputDir.get().asFile.absolutePath,
        "--main-jar", mainJarName,
        "--main-class", "dev.velofine.launcher.Main",
        "--name", appName,
        "--app-version", project.version.toString(),
        "--vendor", "siliconcode-dev",
        "--icon", file("branding/velofine.ico").absolutePath,
        "--dest", jpackageOutputDir.get().asFile.absolutePath
    )
}

val innoSetupCompile = tasks.register<Exec>("innoSetupCompile") {
    dependsOn(jpackageAppImage)
    inputs.dir(jpackageOutputDir)
    inputs.file("installer.iss")
    outputs.dir(innoOutputDir)

    onlyIf {
        if (!OperatingSystem.current().isWindows) {
            logger.warn("innoSetupCompile skipped: Inno Setup only runs on Windows")
            false
        } else {
            true
        }
    }

    doFirst {
        delete(innoOutputDir)
        mkdir(innoOutputDir)
    }

    val isccCandidates = listOf(
        "C:\\Program Files (x86)\\Inno Setup 6\\ISCC.exe",
        "C:\\Program Files\\Inno Setup 6\\ISCC.exe",
        "${System.getenv("LocalAppData")}\\Programs\\Inno Setup 6\\ISCC.exe"
    )
    val iscc = isccCandidates.firstOrNull { file(it).exists() } ?: "ISCC.exe"

    commandLine(
        iscc,
        "/DAppVersion=${project.version}",
        "/DJpackageOutputDir=${jpackageOutputDir.get().asFile.absolutePath}",
        "/DOutputDir=${innoOutputDir.get().asFile.absolutePath}",
        file("installer.iss").absolutePath
    )
}
