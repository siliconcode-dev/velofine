// Utility engine: OptiFine-parity QoL features and shader pipeline support.
// Configuration inherited from the root project's `subprojects` block.
//
// Phase 5 brought this module into existence as a real engine (entry point, config panel,
// safe-defaults policy) with no QoL features yet. Phase 6 adds the first ones - Mixin/ASM/Guava
// arrive transitively via core's `api(...)` declarations (see core/build.gradle.kts), same as
// optimus/legacysupport, so no separate Mixin dependency line is needed here.

dependencies {
    implementation(project(":core"))

    // Zoom/Fog mixins reference Camera/FogRenderer/FogData/Options/Minecraft signatures.
    compileOnly(project(":mcstubs"))

    // Phase 7: the shader pipeline itself lives in :shaders (split out given its scope - see
    // shaders/build.gradle.kts); this module owns the engine-toggle wiring and GUI integration.
    implementation(project(":shaders"))
}
