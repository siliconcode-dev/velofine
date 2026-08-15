// Optimus engine: OpenGL-focused performance optimization.
// Configuration inherited from the root project's `subprojects` block.

dependencies {
    // Mixin/ASM/Guava come transitively via core's `api(...)` dependencies (see
    // core/build.gradle.kts) - this module's own mixin classes use those types directly.
    implementation(project(":core"))
}

// mixins.optimus.json declares compatibilityLevel JAVA_16 for the same reason
// legacysupport/build.gradle.kts documents: Mixin 0.8.7's ASM-minor-version auto-detection can't
// read a version back from launcher's shaded/merged jar, so anything above JAVA_16 spuriously
// fails even with real ASM 9.10.1 present.

// OptimusVerifyMixinsHarness (src/test/java) is a manual diagnostic tool, not a JUnit test - same
// reasoning as legacysupport/build.gradle.kts.
tasks.test {
    failOnNoDiscoveredTests.set(false)
}
