# CLAUDE.md — Velofine

This file is guidance for Claude Code when working in this repository. Read `Masterdoc.md` for full project context and `Build_plan.md` for the phased implementation plan before starting work. Always know which phase you're currently in.

## Project summary

Velofine is a standalone, launcher-level OptiFine-style patcher for Minecraft Java Edition 26.2, targeting Windows only. It is not a Fabric/NeoForge mod. It patches the game via a `-javaagent` at the bytecode level, installs as a custom launcher profile, and ships as a single branded `.exe` installer. Three independent engines: **LegacySupport** (ancient-hardware compatibility, especially a documented Intel HD 4000 rendering bug), **Optimus** (performance, OpenGL-focused), and **Utility** (OptiFine-parity QoL features + full shader pipeline support).

## How to work in this repo

- **Follow the phase you're in.** `Build_plan.md` is authoritative for sequencing. Don't build Phase 4 features while Phase 1's exit criteria are unmet. If a phase's exit criteria can't be verified (e.g. no access to the reference legacy hardware), say so explicitly rather than marking it done.
- **Phase 1 (patcher pipeline) and Phase 2 (Intel HD 4000 fix) are the highest-stakes phases.** Phase 1 is the architectural bet the whole project rests on. Phase 2 is a real, personally-verified bug affecting a real person (the project owner's friend) — treat correctness here as more important than speed.
- **No access to reference legacy hardware.** The project owner does not personally own the i3-3110M / Intel HD 4000 or i5-3470S / Intel HD Graphics 2500 test machines — validation of LegacySupport fixes depends on two friend/community testers, one per machine. When implementing LegacySupport features, write clear manual test/verification steps (what to check in the F3 debug screen, what log output confirms the fix engaged, etc.) so a non-developer tester can confirm results.
- **Keep engines decoupled from the start**, even before the toggle UI exists (that's Phase 5). LegacySupport, Optimus, and Utility should be structured as independently buildable/runnable modules from Phase 1 onward.
- **Never let a feature silently degrade vanilla fidelity.** This applies especially to LegacySupport's memory-saving work (Phase 3) and Utility's texture/CTM features — "vanilla+" means vanilla behavior stays intact unless the user has opted into a change.

## Tech stack & conventions

- **Build tool:** Gradle, single monorepo with modules: `core` (shared utilities: config system, logging, GPU/hardware detection, `AgentContext`), `legacysupport`, `optimus`, `utility`, `launcher` (the self-attaching wrapper jar + installer CLI), `installer` (jpackage/Inno Setup packaging tooling, no Java source).
- **Language/runtime:** Java, targeting Java 25 (Minecraft 26.1+'s requirement). Confirm the exact minimum Java version Velofine itself requires vs. what it bundles for end users — these can differ.
- **Patching approach:** Runtime bytecode transformation via a self-attaching `-javaagent` (`launcher` module's `Main`/`VelofineAgent`, using the JDK Attach API + `-Djdk.attach.allowAttachSelf=true`, set via the generated profile's `javaArgs` — not a literal `-javaagent:` flag).
- **Mixin tooling decision (resolved Phase 1, implemented Phase 2):** SpongePowered Mixin (`org.spongepowered:mixin:0.8.7`), chosen over plain ASM on the strength of SpongeVanilla's precedent. A real custom `IMixinService` now exists (`legacysupport/.../mixin/VelofineMixinService.java`) and is verified working against real vanilla 26.2 bytecode — see `legacysupport/src/test/java/.../diagnostic/VerifyMixinsHarness.java`, a manual diagnostic (not JUnit) that applies our mixins directly to `GlBackend.class`/`GlDevice.class` extracted from the real client jar and confirms via bytecode inspection. Key gotchas discovered building this (all now fixed, but relevant if this area breaks again):
  - `Mixins.addConfiguration(String)` (1-arg) passes a `null` fallback `MixinEnvironment` internally and NPEs in `MixinConfig.onLoad()`. Use the 2-arg overload (`addConfiguration(file, (IMixinConfigSource) null)`), which resolves `MixinEnvironment.getDefaultEnvironment()` properly.
  - `MixinServiceAbstract`'s default `getInitialPhase()` (`Phase.PREINIT`) doesn't match the `Phase.DEFAULT` environment `addConfiguration`'s fallback resolves to — two different `MixinEnvironment` objects, and `MixinConfig#select`'s reference-equality check silently drops the config (no error, mixins just never apply). `VelofineMixinService` overrides `getInitialPhase()` to return `Phase.DEFAULT` to fix this. Side effect: Mixin logs a scary-looking but harmless `ERROR: Initialising mixin subsystem after game pre-init phase!` — confirmed empirically this doesn't block anything for us (no FML-style staged pre-init phase to skip).
  - Mixin 0.8.7's `CompatibilityLevel` ceiling is `JAVA_21`, but its ASM-minor-version auto-detection (`Package.getImplementationVersion()`) can't read a version back from `launcher`'s shaded/merged jar, so levels needing an ASM minor-version check (anything above `JAVA_16`, which needs only ASM major version ≥9) spuriously fail even with real ASM 9.10.1 present. `mixins.legacysupport.json` uses `compatibilityLevel: JAVA_16` for this reason.
  - Mixin's own POM declares zero dependencies despite needing ASM (all 5 artifacts: asm, asm-commons, asm-tree, asm-util, asm-analysis) and Guava at runtime — wired explicitly in `legacysupport/build.gradle.kts`. ASM 9.10.1 specifically, since real target classes are Java 25 bytecode (class file version 69, needs ASM 9.8+).
  - `mixins.legacysupport.json` needs `-Dmixin.env.disableRefMap=true` (now added to every generated profile's `javaArgs` by `ProfileInstaller`) since we don't use a refmap (real Mojang names, not SRG-obfuscated).
  - `@Mixin(targets = "string")` (not `@Mixin(RealClass.class)`) throughout, since we can't depend on Mojang's client jar at compile time (proprietary, not republishable). Real Minecraft types referenced in mixin method *signatures* (e.g. `GlDeviceMixin`'s `@Redirect` handler needs `ShaderSource`/`ShaderType`/`Identifier`) are satisfied by hand-authored, signature-only stub classes in `legacysupport/src/stubs/java/` (a separate `compileOnly` Gradle source set, never bundled) — zero decompiled Mojang logic, just matching package/class/method names so `javac` can resolve them.
- **Installer packaging:** `jpackage --type app-image` (bundles a Java 25 runtime via jlink) → **Inno Setup 6.6+** for the final Windows `.exe` (`installer/installer.iss`, run via the `:installer:jpackageAppImage`/`:installer:innoSetupCompile` Gradle tasks). UI direction: **black / white / red palette, brutalist design language** — Inno Setup 6.6+ supports this natively (`WizardStyle=dark`, `WizardBackColor`, custom `WizardImageFile`/`WizardSmallImageFile`), no hacky workarounds needed. Apply consistently across installer screens and any in-game Velofine UI (config panels, benchmark overlay), not just the installer. Installs per-user to `{localappdata}\Velofine` — no admin/UAC required. Source logo art drops into `assets/`; the actual consumed, pre-sized files live in `installer/branding/` (currently placeholder art — see `assets/README.md`).
- **License:** LGPL for the whole project. Apply LGPL license headers consistently. If/when Phase 7 (shaders) adapts components from Iris (also LGPL), keep attribution and licensing clean — do not silently vendor code without proper LGPL compliance (source availability, license notices).
- **No telemetry, ever.** No crash reporting, no analytics, opt-in or otherwise. Do not add any network call that phones home data about the user's system beyond the explicitly-designed in-app updater checking for new releases.
- **No code signing for v1.** Windows SmartScreen warnings on the unsigned installer/updater are expected and accepted — don't try to work around this with anything other than clear README/installer messaging.

## Testing

- Manual QA is acceptable through Phase 1–8. Don't block early phases on test coverage.
- Starting Phase 9, add automated unit/integration tests for the now-stabilized core: patcher pipeline, each engine's core logic, config system.
- CI (build pipeline) should exist from Phase 0 onward regardless of test coverage maturity — every push should at least compile and package successfully.

## Local dev testing: game directory

**Always use the project owner's real Minecraft directory for local install/launch testing on this
machine — do not use a synthetic `/tmp` fixture or the default `%APPDATA%\.minecraft` path:**

```text
C:\Users\Azam\AppData\Roaming\.tlauncher\legacy\Minecraft\game
```

This is a real, standard-format (`versions/` + `launcher_profiles.json`) game directory managed by
Legacy Launcher Stable (a TLauncher sibling) — not the official Mojang launcher, but Velofine reads
and writes the same file layout either way. Vanilla 26.2 is being installed here now; once it's
present at `versions\26.2\26.2.json`, this becomes the real end-to-end test target for Phase 1's
exit criteria (install → launch the Velofine profile → title screen → confirm the agent-attached log
line) and every LegacySupport/Optimus/Utility feature after it. Before that, this folder only has
`Fabric 1.19.4` installed — useful for structural JSON testing (see CLAUDE.md git history / commit
`1d1f62e`) but not a substitute for a real vanilla 26.2 run.

When testing `ProfileInstaller` (`--install-profile`/`--uninstall-profile`) against this folder,
prefer a clean install → verify → uninstall round-trip unless the project owner asks for the profile
to be left in place — it's their real, in-use game directory, not disposable scratch space.

## Documentation

- Maintain `README.md` starting Phase 1 with install instructions and screenshots/GIFs; keep it current as features ship in later phases.
- `CONTRIBUTING.md` and a formal code style guide are intentionally deferred — do not create these until Phase 9 unless explicitly asked earlier.
- Keep a public-facing roadmap (GitHub Projects board) reasonably in sync with `Build_plan.md`'s phases.

## Things explicitly out of scope (do not build unless the project owner changes direction)

- Fabric/NeoForge/mod-loader coexistence or compatibility layers.
- macOS/Linux support.
- Vulkan rendering integration in Optimus (OpenGL-focused by design).
- Any telemetry, analytics, or crash reporting.
- Code signing / paid certificates.
- Server-side (non-client) features — Velofine only needs to work seamlessly as a client connecting to vanilla multiplayer servers, not add server-side functionality.

## LegacySupport: real findings (Phase 2, grounded in the real 26.2.jar via javap)

- `com.mojang.blaze3d.opengl.GlBackend.setWindowHints()` is the exact, isolated method requesting the GL context. Vanilla requests **OpenGL 3.3, Core Profile, Forward-Compatible**. `GlBackendMixin` `@Overwrite`s it: on a detected fix profile, requests 3.3 **Compatibility** Profile with forward-compat dropped instead (same version, superset of core, shouldn't break anything core-profile-only code does).
- `com.mojang.blaze3d.opengl.GlDevice.compileShader(...)` calls `ShaderSource.get(Identifier, ShaderType)` to obtain GLSL source text — the one place to intercept for the shader `mix()` patch. `GlDeviceMixin` `@Redirect`s that single call.
- `GpuDetector` (`core` module) identifies Intel Gen7 (HD Graphics 4000/2500) via `Get-CimInstance Win32_VideoController` (PowerShell/WMI) adapter-name matching — not Sodium's native D3DKMT/`ig7icd*.dll` approach, which needs JNA/native calls we deliberately avoided for a Windows-only v1.
- Both fixes are **hypotheses grounded in real research** (OptiFine's historical mirror-image Intel GL-profile issue; an Intel Community forum thread on `mix()` constant-folding bugs), not confirmed against the actual reference hardware — nobody on this project has access to it. Verified so far: the Mixin pipeline genuinely transforms the real target classes correctly (see `VerifyMixinsHarness`) and the generated profile launches clean on unaffected hardware. **Not yet verified:** whether these fixes actually resolve the invisible-portals/lava/water bug on the i3-3110M/i5-3470S reference machines — that's the real acceptance test per Build_plan, pending the two testers.

## LegacySupport: generalized fix system + Phase 3 findings (grounded in the real 26.2.jar via javap)

- The Phase 2 pattern of hand-written boolean methods on `LegacySupportEngine` branching on a single flat `GpuInfo.FixProfile` enum has been replaced with a composite model in `core`'s new `dev.velofine.core.hardware` package: `HardwareProfile` (GPU + `MemoryInfo` + `DiskInfo`, each with its own PowerShell/WMI-based detector following `GpuDetector`'s exact shape) resolves via `FixProfileRules` (a short list of independent, additive `(predicate, fixes)` rules) to a `Set<Fix>`. Every mixin now checks `LegacySupportEngine.isFixActive(Fix.X)` instead of a bespoke boolean getter — adding a new hardware class going forward means adding one `FixProfileRules` entry, not touching call sites. The dev-testing hook is now `-Dvelofine.legacysupport.forceFixes=<comma-separated Fix names>` (replaces Phase 2's `forceProfile`).
- `MemoryDetector` classifies ≤6GB total physical RAM as "low-memory" (headroom above the documented 4GB reference machines, since real 4GB sticks report somewhat under 4×1024³ bytes). `DiskDetector` resolves the game directory's drive letter → partition → disk number → `Get-PhysicalDisk`'s `MediaType` to detect a rotational HDD; the game directory itself is threaded through as a new `-Dvelofine.gameDir` system property, set by `Main.captureGameDir()` from the real `--gameDir` launch arg *before* self-attaching (same JVM, so the property is visible to the agent) — not baked into the generated profile JSON, since the actual directory is only known at each real launch, not at profile-generation time.
- `net.minecraft.server.level.ChunkMap.saveChunksEagerly(BooleanSupplier)` throttles eager dirty-chunk saves via a real vanilla constant (`CHUNK_SAVED_EAGERLY_PER_TICK`, confirmed `ConstantValue: int 20`, inlined by javac at its one use site in this method with no other colliding `20` literal). `ChunkMapMixin` `@ModifyConstant`s it down to 6 when `IO_STALL_SMOOTHING` is active, reducing how many saves get queued against the same single-threaded IO executor that also services chunk loads on a rotational disk.
  - **Investigated and deliberately rejected:** directly reordering `IOWorker`'s load/save priority (the originally-planned approach). javap showed both `store(...)` and `loadAsync(...)` funnel through one private `submitTask` method with `IOWorker.Priority.FOREGROUND` hardcoded via a `getstatic` *inside* `submitTask` itself — not passed by the caller — so there's no caller-side seam to redirect safely. The only way to give saves a different priority would be reimplementing `submitTask`'s internal `scheduleWithResult` callback wiring by referencing its synthetic lambda method (`lambda$submitTask$0`) by name — an unstable javac implementation detail, not a real API, and a bad foundation for a shipped correctness-sensitive feature (this is chunk-save code; a wiring mistake risks corrupting player worlds). `ChunkMap`'s named, single-purpose constant was the safer real lever.
- `net.minecraft.client.Options`'s constructor sets first-run defaults for `renderDistance`/`simulationDistance`/`entityDistanceScaling`/`mipmapLevels`/`particles` (all confirmed real `OptionInstance<T>` fields). `OptionsMixin` lowers these when `MEMORY_SAVING_DEFAULTS` is active, via `@ModifyConstant`/`@Redirect` scoped with Mixin's `@Slice` between each option's own translation-key string constant (e.g. `"options.mipmapLevels"`) and its field assignment — necessary because the constructor is one ~5000-instruction method covering every video setting, with several colliding literal values (e.g. `12` is the default for *both* render and simulation distance; `4` appears twice just for mipmapLevels' own range-max and default). Slice-bounding by stable, named anchors (rather than a raw global ordinal) means an unrelated constant added elsewhere in a future Minecraft version can't silently shift which value gets modified — Mixin fails loudly at apply time if a slice boundary goes missing instead. Verified via `VerifyMixinsHarness` (extended for `ChunkMap`/`Options`) that all five injections land on the intended instruction — confirmed by hand for the two trickiest cases (`mipmapLevels`' ordinal disambiguation, `particles`' field redirect) by disassembling the transformed class and checking the injected call sits exactly at the default-value slot, not the range-bound slot.
  - This only ever changes what a *fresh* install starts at: vanilla's own `options.txt` load (immediately after construction) overwrites any of these fields if the file already exists, so no explicit "does options.txt exist" check was needed in the mixin — vanilla's load order provides that gate for free.
  - Chosen low-memory defaults: render/simulation distance 6, entity distance scaling 0.5 (its range minimum), mipmap levels 0, particles `DECREASED` (not `MINIMAL` — a conservative starting point per the project owner's explicit choice this phase).
- True dynamic texture-atlas streaming (load/evict on demand) was explicitly scoped out of Phase 3 — too risky to build and validate without reference hardware. The mipmap-level default above is the only texture-memory lever shipped this phase.
- Like Phase 2, these are **hypotheses grounded in real research and verified-correct bytecode transforms**, not yet confirmed to reduce stutter/memory footprint on the actual i3-3110M/i5-3470S reference machines — pending the two testers, same as Phase 2.

## Open decisions to resolve during implementation (not blockers to starting)

- Whether the GL-compat-profile + shader-patch hypotheses above actually fix the bug on real reference hardware (Phase 2's actual acceptance test — tester-dependent, see above).
- Whether Phase 3's eager-chunk-save throttling and first-run video-setting defaults actually reduce stutter/memory footprint on real reference hardware (tester-dependent, see above).
- Whether 26.2's bundled LWJGL version (confirmed 3.4.1) independently contributes to the legacy-hardware rendering bug, beyond the GL context/shader issues already addressed.
- Whether to build the shader-format parser from scratch or adapt Iris's LGPL components (Phase 7 decision point).
- Whether to ship an automatic crash-recovery "safe mode" (Phase 9 revisit).
- Whether the mipmap-only texture-memory lever is sufficient or a real texture-atlas streaming system is worth the risk in a future phase (Phase 3 finding, revisit if testers report continued memory pressure).

When any of these get resolved, update `Masterdoc.md`'s "Open Questions" section and this file so future work (and future Claude Code sessions) stay in sync with the decision.
