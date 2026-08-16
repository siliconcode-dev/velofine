// LegacySupport engine: ancient-hardware compatibility fixes.
// Configuration inherited from the root project's `subprojects` block.

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

    // Signature-only Minecraft stubs. Phase 5 moved these out of this module's own
    // `src/stubs/java` source set into the shared :mcstubs module - see mcstubs/build.gradle.kts
    // for why that per-engine decision from Phase 4 was reversed. compileOnly, so shadowJar can
    // never bundle a fake net.minecraft.* class over the real one.
    compileOnly(project(":mcstubs"))

    // GlBackendMixin's method body references real GLFW constants - Mixin's target-resolution
    // step needs to load that class's metadata via the classloader (confirmed by
    // VerifyMixinsHarness's own class javadoc), which only works if the real jar is genuinely on
    // the classpath, not just compileOnly. testRuntimeOnly since production always gets LWJGL from
    // Minecraft's own classpath (see the compileOnly comment above) - this is purely so
    // VerifyMixinsHarness's live JUnit run can resolve it.
    testRuntimeOnly("org.lwjgl:lwjgl:3.4.1")
    testRuntimeOnly("org.lwjgl:lwjgl-glfw:3.4.1")
}

// mixins.legacysupport.json declares compatibilityLevel JAVA_16, not the higher JAVA_21 Mixin
// 0.8.7 formally supports: confirmed via VerifyMixinsHarness that Mixin's ASM-minor-version
// auto-detection (Package.getImplementationVersion()) can't read a proper version back from
// launcher's shaded/merged jar, so any level requiring an ASM minor-version check beyond "major
// version only" spuriously fails even though real ASM 9.10.1 is present. JAVA_16 only checks the
// ASM major version (>=9), which detects correctly, and comfortably covers the plain Java syntax
// our mixins actually use.

// Phase 9: VerifyMixinsHarness is now a real JUnit test (see its own class javadoc for how it
// conditionally attaches the real agent) - runs live when -Pvelofine.test.mcJarPath/
// VELOFINE_TEST_MC_JAR points at a real 26.2 client jar, SKIPs cleanly otherwise. That wiring
// lives here since it needs :launcher's build output.
tasks.test {
    val mcJarPath = (findProperty("velofine.test.mcJarPath") as String?) ?: System.getenv("VELOFINE_TEST_MC_JAR")
    if (mcJarPath != null) {
        dependsOn(":launcher:shadowJar")
        jvmArgs("-javaagent:${project(":launcher").tasks.named("shadowJar").get().outputs.files.singleFile}")
        systemProperty("velofine.legacysupport.forceFixes",
            "GL_COMPATIBILITY_PROFILE,SHADER_MIX_PATCH,IO_STALL_SMOOTHING,MEMORY_SAVING_DEFAULTS")
        systemProperty("velofine.test.mcJarPath", mcJarPath)
        classpath += files(mcJarPath)
    }
}
