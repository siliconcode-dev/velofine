// Shaders module: OptiFine/Iris-format shader pack support (Phase 7).
// Configuration inherited from the root project's `subprojects` block.
//
// Split out of `utility` into its own module (rather than a subpackage, unlike optimus/.../governor)
// because of sheer scope - format parsing, an options data model, framebuffer/render-target
// management, and the gbuffers/deferred/composite/shadow pipeline itself are each individually
// larger than most existing engine modules. `utility` depends on this module and owns the
// engine-toggle wiring/GUI integration; Masterdoc 4.3 frames shaders as a Utility Engine feature, so
// there is no separate `engines.shaders` toggle - see VelofineConfig.UtilitySection.ShaderSection.
//
// Some logic here is adapted from IrisShaders/Iris (https://github.com/IrisShaders/Iris), LGPL-3.0,
// per the project owner's explicit Phase 7 decision - see CLAUDE.md's shader research findings for
// which files/packages and why. Adapted files carry their own attribution header citing the
// original Iris source. Iris's own AGPL-3.0 `glsl-transformer` dependency is deliberately NOT used
// here (see below); `org.anarres:jcpp` is Iris's *other* preprocessing dependency (Apache-2.0, no
// license conflict) and is depended on directly for #define/#ifdef handling.

dependencies {
    implementation(project(":core"))

    // Shader pack parsing/pipeline classes reference Minecraft/Blaze3D signatures (RenderTarget,
    // RenderPipeline, GameRenderer, Camera, Options, ShaderSource/ShaderType, ...). compileOnly,
    // never shaded - same rule every other engine module follows.
    compileOnly(project(":mcstubs"))

    // AlphaTestFunction/BlendModeFunction reference GL11 constants (not calling into GL directly -
    // just reusing Mojang's own constant values so blend/alphaTest directives parse to real GL
    // enums). Real LWJGL comes from Minecraft's own classpath at runtime, same as legacysupport.
    compileOnly("org.lwjgl:lwjgl:3.4.1")
    compileOnly("org.lwjgl:lwjgl-opengl:3.4.1")

    // C-preprocessor-style #define/#ifdef handling for shaders.properties and shader source, the
    // same library Iris itself uses for this (distinct from and NOT to be confused with Iris's
    // AGPL-3.0 glsl-transformer, which this project does not depend on).
    implementation("org.anarres:jcpp:1.4.14")
}

tasks.test {
    failOnNoDiscoveredTests.set(false)
}
