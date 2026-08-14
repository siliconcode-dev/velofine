import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    alias(libs.plugins.spotless) apply false
}

val javaVersion = libs.versions.java.get().toInt()

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")

    group = "dev.velofine"
    version = "0.0.1-phase0"

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
    }

    repositories {
        mavenCentral()
    }

    configure<SpotlessExtension> {
        lineEndings = com.diffplug.spotless.LineEnding.UNIX
        java {
            target("src/**/*.java")
            licenseHeaderFile(rootProject.file("HEADER.txt"))
        }
    }

    tasks.named("check") {
        dependsOn("spotlessCheck")
    }
}
