// LegacySupport engine: ancient-hardware compatibility fixes.
// Configuration inherited from the root project's `subprojects` block.

// A separate, compile-only source set for hand-authored stub types matching real Minecraft
// classes' package/name/method *signatures* only (zero decompiled Mojang logic - just enough for
// javac to resolve @Redirect handler parameter types in the mixins below). Never bundled: not a
// dependency of any runtime configuration, so shadowJar never sees it. We can't depend on
// Mojang's actual client jar (proprietary, not republishable, not something CI has access to
// anyway) - this is the standard workaround modding tooling solves via full deobfuscation
// pipelines; ours is a minimal hand-written version scoped to the handful of types we touch.
sourceSets {
    create("stubs") {
        java.srcDir("src/stubs/java")
    }
}

dependencies {
    // Mixin/ASM/Guava come transitively via core's `api(...)` dependencies (see
    // core/build.gradle.kts) - `api`, not `implementation`, because this module's own mixin
    // classes use those types directly, not just core's MixinBridge/VelofineMixinService. Phase 4
    // hoisted the shared Mixin service plumbing
    // (MixinBridge/VelofineMixinService/VelofinePropertyKey/VelofinePropertyService) into core so
    // legacysupport and optimus share one ServiceLoader-registered transformer pipeline instead
    // of each registering a competing one.
    implementation(project(":core"))

    // Real LWJGL version bundled by vanilla 26.2 (confirmed via its version JSON) - compileOnly
    // because at runtime this comes from Minecraft's own classpath; bundling our own copy would
    // risk a duplicate/conflicting LWJGL (and its native libraries) alongside vanilla's.
    compileOnly("org.lwjgl:lwjgl:3.4.1")
    compileOnly("org.lwjgl:lwjgl-glfw:3.4.1")

    compileOnly(sourceSets["stubs"].output)
}

// mixins.legacysupport.json declares compatibilityLevel JAVA_16, not the higher JAVA_21 Mixin
// 0.8.7 formally supports: confirmed via VerifyMixinsHarness that Mixin's ASM-minor-version
// auto-detection (Package.getImplementationVersion()) can't read a proper version back from
// launcher's shaded/merged jar, so any level requiring an ASM minor-version check beyond "major
// version only" spuriously fails even though real ASM 9.10.1 is present. JAVA_16 only checks the
// ASM major version (>=9), which detects correctly, and comfortably covers the plain Java syntax
// our mixins actually use.

// VerifyMixinsHarness (src/test/java) is a manual diagnostic tool run directly via `java`, not a
// JUnit test - CLAUDE.md's Testing section says manual QA is fine through Phase 8. Without this,
// Gradle's default `test` task fails the build for finding test sources but no discoverable tests.
tasks.test {
    failOnNoDiscoveredTests.set(false)
}

