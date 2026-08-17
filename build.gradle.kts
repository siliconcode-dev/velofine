import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    alias(libs.plugins.spotless) apply false
}

val javaVersion = libs.versions.java.get().toInt()
val junitVersion = libs.versions.junit.get()
val mockitoVersion = libs.versions.mockito.get()

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")
    // Phase 9: report generation only (jacocoTestReport), no jacocoTestCoverageVerification gate -
    // this phase is catching up test debt across an already-shipped v1-8 codebase, not imposing a
    // coverage bar that would block unrelated future commits.
    apply(plugin = "jacoco")

    group = "dev.velofine"
    version = "1.5-Beta"

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

    // Centralized here (was previously hand-duplicated in legacysupport/optimus/utility/shaders'
    // own build.gradle.kts) since every module now potentially has real tests, not just the ones
    // that happened to declare the guard before Phase 9. String-invoke form ("testImplementation")
    // rather than the typed extension, since `java-library` is applied above at runtime but this
    // script itself never applies it to the root project, so the typed accessor isn't visible at
    // this script's own compile time. Plain coordinate strings (versions captured above), not the
    // `libs.` catalog accessor - that generated extension isn't registered on the subproject
    // instances this closure configures, only on the root project itself.
    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:$junitVersion")
        "testImplementation"("org.mockito:mockito-core:$mockitoVersion")
        "testImplementation"("org.mockito:mockito-junit-jupiter:$mockitoVersion")
        // Gradle no longer bundles this - required on the test runtime classpath to actually
        // execute JUnit 5 tests, independent of the junit-jupiter engine/API dependency above.
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // A module with no test sources at all (mcstubs, installer) must not fail `check` just for
        // having none - same reasoning the four per-module copies of this flag already documented.
        failOnNoDiscoveredTests.set(false)
    }

    tasks.named("check") {
        dependsOn("spotlessCheck")
    }
}
