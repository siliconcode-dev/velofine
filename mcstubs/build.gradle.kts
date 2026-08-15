// Hand-authored, signature-only stub types matching real Minecraft/Blaze3D classes' package,
// name and method *signatures* only - zero decompiled Mojang logic. They exist purely so javac can
// resolve the types our mixin handlers and GUI classes reference; at runtime the real classes come
// from Minecraft's own classpath.
//
// Phase 5 note - this REVERSES Phase 4's "keep stubs per-engine" decision, deliberately. That call
// was made when four tiny stubs lived in legacysupport alone ("the handful of stubs so far cost
// little to duplicate"). Phase 5's in-game config UI needs ~20 more, and `core` (the GUI + its
// entry-point mixins), `optimus` (the governor) and `utility` all need the same ones - duplicating
// that three ways is clearly worse than one shared module.
//
// CRITICAL: this module must NEVER appear on a runtime configuration. Every consumer declares it
// `compileOnly`, and `launcher` (the only module that is shaded into a shipped jar) does not depend
// on it at all - so shadowJar can never bundle a fake `net.minecraft.*` class over the real one.
//
// Because these stubs are compiled against but never loaded, only their *erased signatures* matter,
// and they must match the real classes exactly - a mismatched erasure produces a methodref that
// resolves fine at compile time and throws NoSuchMethodError at runtime. Every signature here was
// taken from `javap` output against the real 26.2 client jar. Where a real method is generic, the
// stub keeps the same type-variable bounds, because the *first* bound determines the erasure
// (e.g. Screen.addRenderableWidget's `<T extends GuiEventListener & Renderable & NarratableEntry>`
// erases to GuiEventListener, not to AbstractWidget).
//
// The supertype hierarchy is deliberately NOT mirrored: interfaces like GuiEventListener are
// declared as empty markers rather than reproducing their ~18 default methods, since nothing we
// write calls them. Field/method references resolve against the real hierarchy at runtime.

dependencies {
    // InputConstants/Window stubs reference GLFW-adjacent types only by name, but LegacySupport's
    // GlBackendMixin (which now compiles against this module) uses org.lwjgl.glfw.GLFW directly.
    compileOnly("org.lwjgl:lwjgl:3.4.1")
    compileOnly("org.lwjgl:lwjgl-glfw:3.4.1")
}
