// Shared cross-engine utilities (config system, logging, GPU/hardware detection, Mixin service
// plumbing). Configuration inherited from the root project's `subprojects` block.

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")

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
}
