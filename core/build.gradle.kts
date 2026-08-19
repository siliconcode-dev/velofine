// Shared cross-engine utilities (config system, logging, GPU/hardware detection, Mixin service
// plumbing, and - since Phase 5 - the in-game config UI). Configuration inherited from the root
// project's `subprojects` block.

// Phase 8: the single source of truth for "what version am I" (BuildInfo) is generated here from
// project.version - not a second hardcoded literal in ProfileInstaller like before. targetMcVersion
// defaults to 26.2 but is a project property so a future parallel MC-version track can override it
// (`-PtargetMcVersion=...`) without touching this file.
val targetMcVersion = (findProperty("targetMcVersion") as String?) ?: "26.2"
// Captured at configuration time, not read inside the execution-time `expand` closure below -
// `Task.project` is deprecated for execution-time access (breaks the configuration cache).
val velofineVersionForResource = project.version.toString()

tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    inputs.property("velofineVersion", velofineVersionForResource)
    inputs.property("targetMcVersion", targetMcVersion)
    filesMatching("velofine-build-info.properties") {
        expand(
            "velofineVersion" to velofineVersionForResource,
            "targetMcVersion" to targetMcVersion
        )
    }
}

dependencies {
    // `api`, not `implementation`, since Phase 5: dev.velofine.core.config types are read and
    // mutated by every engine module and by the UI, and ConfigManager exposes Gson-serialized
    // POJOs across that boundary.
    api("com.google.code.gson:gson:2.14.0")

    // Mixin service/transformer plumbing (MixinBridge, VelofineMixinService,
    // VelofinePropertyKey/Service) lives here, not in any one engine module: ServiceLoader's
    // META-INF/services registrations and Mixin's MixinBootstrap.init()/Mixins.addConfiguration()
    // are global per-JVM, so every engine (legacysupport, optimus, ...) shares this one pipeline
    // rather than each registering a competing IMixinService/IGlobalPropertyService (moved here
    // from legacysupport's build.gradle.kts in Phase 4, when optimus needed the same plumbing).
    //
    // Mixin's own published POM declares zero dependencies despite using both throughout its
    // source - callers are expected to supply them. ASM 9.10.1 specifically: real target classes
    // (e.g. GlBackend/GlDevice/ChunkMap/Mob) are compiled at Java 25 bytecode (class file version
    // 69), which needs ASM 9.8+ to parse at all; 9.10.1 is also exactly what real Fabric/Mixin
    // tooling ships together (confirmed in this machine's own Fabric 1.19.4 libraries list).
    //
    // `api`, not `implementation`: every engine module authors its own `@Mixin`-annotated classes
    // (using org.spongepowered.asm.mixin.* / org.objectweb.asm.* directly in its own source), not
    // just consuming core's MixinBridge/VelofineMixinService - `implementation` would hide these
    // from an engine's compile classpath and break every mixin class in legacysupport/optimus.
    api("org.spongepowered:mixin:0.8.7")
    api("org.ow2.asm:asm:9.10.1")
    api("org.ow2.asm:asm-commons:9.10.1")
    api("org.ow2.asm:asm-tree:9.10.1")
    api("org.ow2.asm:asm-util:9.10.1")
    api("org.ow2.asm:asm-analysis:9.10.1")
    api("com.google.guava:guava:33.6.0-jre")

    // Signature-only Minecraft stubs for the Phase 5 config UI (dev.velofine.core.gui) and its
    // entry-point mixins. compileOnly on purpose: these fake net.minecraft.* classes must never
    // reach a runtime classpath, where they would shadow the real ones.
    compileOnly(project(":mcstubs"))

    // v1.8-Beta: ShaderSourceInterceptorsTest needs ShaderType/Identifier to exist at *test* runtime.
    // The load-bearing invariant is unchanged - `launcher`, the only module shaded into a shipped
    // jar, still has no dependency on :mcstubs at all, and a test runtime classpath is never shaded -
    // but note this is a deliberate, narrow relaxation of mcstubs' "never on a runtime configuration"
    // wording, not an oversight. See mcstubs/build.gradle.kts's header.
    testImplementation(project(":mcstubs"))
}
