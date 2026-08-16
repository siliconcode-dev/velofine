// Optimus engine: performance optimization, OpenGL-focused.
// Configuration inherited from the root project's `subprojects` block.

dependencies {
    // Mixin/ASM/Guava arrive transitively via core's `api(...)` declarations - see
    // core/build.gradle.kts for why those are `api` and not `implementation`.
    implementation(project(":core"))

    // Phase 5's performance governor reads Minecraft.getFps() and writes
    // Options.renderDistance().set(...), so unlike Phases 4's mixins (primitives only) this module
    // now needs the signature-only Minecraft stubs. compileOnly - never shaded.
    compileOnly(project(":mcstubs"))
}

// Optimus's mixins are verified by legacysupport's VerifyMixinsHarness (which loads both engines'
// mixin configs into one JVM on purpose - that is also how the shared MixinBridge transformer gets
// exercised with more than one config). Phase 9 adds real unit tests for this module's pure logic
// (PerformanceGovernor/FpsSampler) instead - see src/test/java.
