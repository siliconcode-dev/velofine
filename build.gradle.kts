import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    alias(libs.plugins.spotless) apply false
}

val javaVersion = libs.versions.java.get().toInt()

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")

    group = "dev.velofine"
    version = "0.1.0"

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
    }

    repositories {
        mavenCentral()
        // SpongePowered Mixin isn't on Maven Central. Declared once here (not per-module) since
        // Gradle resolves transitive dependencies using the *consuming* project's repositories,
        // not the declaring project's - launcher transitively needs this via legacysupport even
        // though launcher itself never mentions Mixin directly.
        maven("https://repo.spongepowered.org/repository/maven-public/")
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
