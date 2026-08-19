# CLAUDE.md — Velofine 1.5 Update

This is a focused addendum for the **v1.5 update work** on top of the existing shipped Velofine v1 codebase. Read `Masterdoc_v1.5.md` for full context and `Build_plan_v1.5.md` for phase sequencing before starting. If a repo-root `CLAUDE.md` already exists from v1, treat this as the currently-active supplement while v1.5 work is in progress — don't discard v1's original conventions (license, no-telemetry, no-signing, etc.), which all still apply.

## What this update is

v1.5 ships an actual fix for the flagship LegacySupport bug: on ancient Intel-iGPU hardware (Ivy Bridge-era HD 2500/HD 4000 family), Minecraft 26.2 renders lava/water/portals as fully invisible and some blocks as fully black. v1 shipped without this fix; v1.5 delivers it based on founder-led root-cause research (see Masterdoc §2). **Version label: `Velofine 1.5 MC 26.2`.**

## How to work on this update

- **Extend the existing v1 LegacySupport module in place.** Don't rewrite it. If the new detection/registry system doesn't fit the existing structure cleanly, say so explicitly rather than silently restructuring broadly.
- **v1.5 is scoped to LegacySupport + updater/diagnostic tooling only.** Do not touch Optimus or Utility — broader Optimus work is v2, out of scope here.
- **Development is happening blind.** No one on the project has direct access to the affected hardware. The diagnostic export tool (Phase 1) and the tester feedback loop are the actual source of truth — prioritize getting real data over theorizing further from shader source alone. When implementing the shader fix in Phase 4, treat the root-cause explanation in the Masterdoc as a working hypothesis informed by strong circumstantial evidence, not a confirmed diagnosis — validate against real diagnostic reports as they come in.
- **Confidence-tiered fixes matter.** The two verified machines (i3-3110M/HD 4000 + Intel driver 15.33.53.5161; i5-3470S/HD 2500 + Windows-provided driver) get the full targeted shader-patch fix. Broader Ivy Bridge-family matches get only the conservative compatibility-renderer fallback until more field data comes in. Don't apply the aggressive fix to unverified hardware.
- **Everything here ships behind the "Experimental Legacy Fix" toggle**, opt-in for this release (not auto-enabled by default), offered both at install time (if hardware matches) and as an in-game settings toggle.

## Technical specifics

- **Corrected shaders are swappable asset files** (patched `.fsh`/`.vsh` replacing `rendertype_translucent`, `rendertype_end_portal`, `rendertype_solid`, `rendertype_cutout`), not compiled into the agent. This keeps future fix updates patchable without a full app rebuild. Java-side GL-call interception is a fallback only, for cases where the root cause turns out to be buffer setup rather than the shader itself.
- **General shader-robustness improvements apply globally**, regardless of detected hardware: defensive uniform/buffer fallback values, avoiding dynamic (non-constant) array indexing in fragment shaders, universal `GL_DEBUG_OUTPUT`/`KHR_debug`.
- **Detection is two-layer:** pre-context WMI/DXGI (Windows) for early UX (install-time toggle suggestion), post-context `glGetString` confirmation as the actual gate before applying any shader swap — protects against hybrid/switchable-graphics mismatches.
- **No telemetry, still.** `diagnostic.exe` is manual and user-triggered only — it never runs automatically or phones home, and it must not depend on Velofine being installed at all. Output is timestamped JSON, saved locally; the user hands the file to the founder manually. Do not build any automatic submission path in v1.5.
- **`diagnostic.exe` is a standalone monorepo module, not a Velofine feature.** Portable single-file `.exe`, simple GUI, its own independent version number. It asks for the user's Minecraft directory (not Velofine's) to extract the real `rendertype_*` shaders, then actually attempts to compile them via a minimal LWJGL-created OpenGL context, capturing real `GL_COMPILE_STATUS`/info-log output. Build its dual mode (baseline diagnosis + testing candidate fixed shaders sent later during Phase 4) from the start — it's the mechanism the whole Phase 4 tester feedback loop runs on. Bundle it as an extra file in the same release as the main installer.
- **Updater safety:** download → validate → atomic replace via staged temp path; snapshot the previous version and auto-restore only on hard failure (crash/won't launch). Softer regressions are handled by a manual "revert to previous version" button, not auto-rollback — don't try to have the updater infer "this looks visually wrong" on its own.
- **No code signing, still unsigned** — same v1 constraint carries forward.

## Testing

- The founder's friend (owner of the i3-3110M/HD 4000 reference machine) and additional testers are the real test environment — there is no CI-based way to validate the actual fix. Structure changes so a non-developer tester can meaningfully verify results (clear before/after instructions: what to check, what the diagnostic tool should now report).
- Standard CI (build/package) should still pass on every push regardless of hardware-fix validation status.
- No public GitHub issue template for diagnostic submissions yet — that's deferred until after the tester-first rollout phase.

## Definition of done for this update

The two verified reference machines (i3-3110M/HD 4000, i5-3470S/HD 2500) render lava, water, portals, and previously-black blocks correctly with the Experimental Legacy Fix toggle enabled, with no regression versus the pre-fix baseline. Broader family compatibility is a bonus, not a blocker.

## Phase 2: Detection Layer — implemented

- **Confidence-tiered signature registry, `core.gpu.LegacyGpuRegistry`**: a new `GpuConfidence` (`NONE`/`FAMILY_MATCH`/`EXACT_VERIFIED`) is computed alongside `GpuInfo`'s existing (v1, unchanged) `FixProfile` (`INTEL_GEN7`/`GENERIC_OLD`/`NONE`) — additive, not a replacement, so no v1 fix-selection behavior (`FixProfileRules`) changed. `EXACT_VERIFIED` requires: reference machine A — GPU name contains "HD Graphics 4000" **and** driver version equals exactly `15.33.53.5161` **and** CPU name contains "i3-3110M"; reference machine B — GPU name contains "HD Graphics 2500" alone (Masterdoc_v1.5.md S3 documents machine B's driver as "Windows-provided," i.e. no fixed version string exists to gate on). Deliberately mirrors `diagnostics-tool.gpu.DriverQuirkMatcher`'s already-built exact-match rules (v1.5 Phase 1) rather than inventing new criteria, so the standalone diagnostic tool's advisory classification and the live engine's actual fix-eligibility classification agree.
- **CPU detection is new**: `core.gpu.CpuDetector`/`CpuInfo` (WMI `Get-CimInstance Win32_Processor`, mirrors `GpuDetector`'s exact shape) — needed because GPU model + driver alone can't distinguish reference machine A from any other Ivy Bridge laptop sharing the identical HD Graphics 4000 + driver 15.33.53.5161. `HardwareProfile` gained a 4th record component (`cpu`), threaded through `HardwareProfiles.detect()` and `GpuDetector.detect(CpuInfo)` (signature change from v1's no-arg `detect()`).
- **Post-context confirmation needed no new mixin.** `core.mixin.GlDeviceMixin` (Phase 7 of v1) already redirects `GlDevice.compileShader`'s `ShaderSource.get(...)` call through `ShaderSourceInterceptors` — shader compilation cannot happen without a real GL context already current on that thread, so LegacySupport's registered interceptor lambda is already guaranteed to run post-context. `legacysupport.gl.GlContextSignature` captures `GL_VENDOR`/`GL_RENDERER`/`GL_VERSION` lazily (once, cached) from inside that existing seam via plain `GL11.glGetString` calls — no fresh javap research against the real jar was needed for this piece, unlike every prior mixin-adding phase.
- **The actual gate**: `legacysupport.gl.HardwareConfirmation.isConfirmedMatch(HardwareProfile)` correlates the captured `GL_RENDERER` against the WMI-detected adapter name via `core.gpu.AdapterMatcher` (ported from `diagnostics-tool`'s normalize-and-compare algorithm — strips `(R)`/`(TM)`/`(C)` trademark markers as whole tokens before comparing, small intentional duplication rather than a cross-module dependency, same precedent `GpuProbe` already sets). `LegacySupportEngine`'s `Fix.SHADER_MIX_PATCH` interceptor registration now checks this before calling `ShaderPatcher.patch(...)`; on a mismatch (e.g. a hybrid/switchable-graphics laptop where WMI and the bound GL context disagree) it logs a warning and returns `Optional.empty()`, falling through to vanilla shader source exactly like the existing "no fix active" path. `Fix.GL_COMPATIBILITY_PROFILE` (the window-hint fix, `GlBackendMixin`) is untouched — it necessarily runs before any GL context exists, so there's nothing to confirm it against, and Masterdoc_v1.5.md S4 only asks for the gate on shader swaps specifically.
- **Confidence is exposed, not yet consumed by a differentiated fix.** Phase 3 (compatibility-renderer fallback) and Phase 4 (targeted shader fix) don't exist yet, so nothing branches on `EXACT_VERIFIED` vs. `FAMILY_MATCH` today beyond logging — this phase's job was making the signal correct and available, not building its downstream consumers.
- **Known gap, not yet resolved**: no tester has actually dropped a `velofine-diagnosis-*.json` into `testers-diagnosis-report/` yet, so the exact-match classification is validated only against the literal reference-machine values from Masterdoc_v1.5.md S3 (`core.gpu.LegacyGpuRegistryTest`), not a real tester report. Revisit once one arrives.

## v1.8-Beta: the four bugs that made v1.6/v1.7 no-ops in the field

**Both v1.6-Beta's and v1.7-Beta's targeted fixes were confirmed non-functional on real hardware.** A
tester ran v1.7-Beta on reference machine B (i5-3470S / HD Graphics 2500 / driver 10.18.10.5161) and
reported "same as vanilla, nothing changed" — the logs (`testers-diagnosis-report/latest.log (1).txt`,
`launcher.log.txt`) confirm neither fix ever altered a single shader or texture. Four independent root
causes, all confirmed against the real logs plus javap against the real 26.2 jar:

- **mcstubs compile-time constants are inlined, and that silently broke the animated-texture fix.**
  `AnimatedTextureUploadFix` passed `GpuTexture.USAGE_TEXTURE_BINDING | USAGE_COPY_DST`, but the stub
  declared both `= 0` as placeholders. A `static final` primitive *with an initializer* is a JLS 4.12.4
  constant variable, so **javac inlines it into consumer bytecode** — the compiled call literally passed
  `0`, and every upload threw `IllegalStateException: Color texture must have USAGE_COPY_DST to be a
  destination for a write` (63 failures per launch). Real values are `COPY_DST=1, COPY_SRC=2,
  TEXTURE_BINDING=4, RENDER_ATTACHMENT=8, CUBEMAP_COMPATIBLE=16`. Fixed by declaring such stub fields
  **non-final and uninitialized**, forcing a `getstatic` that resolves against the real class at runtime
  and fails loudly (`NoSuchFieldError`) on drift rather than silently computing a wrong value.
  `mcstubs/build.gradle.kts`'s header previously claimed "only their erased signatures matter" — wrong,
  and the direct cause; corrected, and `StubConstantInliningTest` now enforces it mechanically.
- **Shader interception happened one stage too early.** `GlDevice.compileShader` calls
  `ShaderSource.get(...)` at bytecode offset 9 and `GlslPreprocessor.injectDefines(source, defines)` at
  offset 47. `PORTAL_LAYERS` is a `ShaderDefines` value on `RenderPipelines.END_PORTAL` — **not** in the
  raw `.fsh` — so at stage 1 the end-portal patch could not resolve its loop bound and bailed out on
  every launch. `ShaderSourceInterceptors` now has a second, post-`injectDefines` stage
  (`registerPostDefines`/`resolvePostDefines`), driven by a second `@Redirect` in `core.mixin.GlDeviceMixin`;
  shader identity crosses the two stages via a `ThreadLocal` recorded by stage 1 (sound because the two
  calls are straight-line within one `compileShader` invocation, and stage 1's redirect is installed
  unconditionally by `CoreEngine`). `EndPortalArrayIndexPatch` itself needed no code change — only its
  javadoc, which had asserted the `#define` was present in the raw asset. The diagnostic tool's extracted
  `.glsl` files *do* show it, because that tool performs its own define injection — which is exactly why
  the discrepancy went unnoticed, and why `EndPortalArrayIndexPatchTest` now tests raw and post-define
  fixtures separately.
- **Detection could never reach `EXACT_VERIFIED` on the actual reference machine.** WMI reports its
  adapter as the bare `"Intel(R) HD Graphics"` with no model number, which defeated *two* layers:
  `GpuDetector.INTEL_GEN7_PATTERN` (`Intel.*HD Graphics (2500|4000)`) fell through to `GENERIC_OLD`, and
  `LegacyGpuRegistry`'s machine-B signature (name contains "HD Graphics 2500") never matched → only
  `FAMILY_MATCH`. So on a clean config both fixes stayed off entirely; the tester only ever ran them by
  manually forcing the toggles ON. Both layers now accept the CPU model as the decisive signal
  (`i5-3470S`/`i3-3110M` uniquely identify the reference machines; Ivy Bridge only ever shipped HD 2500
  and HD 4000), always conjoined with an Intel-adapter check so a discrete card in a reference-CPU
  machine can't be misclassified. `diagnostics-tool`'s `DriverQuirkMatcher` had the identical blind spot
  and was updated in lockstep, per the mirroring commitment in `LegacyGpuRegistry`'s javadoc.
  The confidence tier itself was **not** broadened — still `EXACT_VERIFIED`-only, deliberately, since
  broadening the gate in the same release as fixing the bugs would make the next tester report
  un-attributable.
- **The auto-updater had never worked on any release.** `GitHubReleaseClient`'s `HttpClient` never set
  `followRedirects`, and Java defaults to `Redirect.NEVER`; GitHub release-asset URLs always 302 to
  `objects.githubusercontent.com`, and both download methods reject anything `!= 200`. Fixed with
  `Redirect.NORMAL` (not `ALWAYS` — it refuses HTTPS→HTTP downgrades). Regression tests reproduce the
  tester's exact `HTTP 302` message. Every existing user must update manually once.

**Also added**: an opt-in `-Dvelofine.shader.dumpPatched=<dir>` export (`core.shader.ShaderSourceDump`)
writing the exact final GLSL handed to the driver, so a tester can compile-check it with
`diagnostic.exe`'s CANDIDATE mode before trusting the live path — closing the Masterdoc Phase 4 loop
without vendoring Mojang shader source into the repo.

**Investigated and dismissed this session**: the 97 `GL_INVALID_VALUE` messages in the tester log all
fire immediately after `Stopping!`, i.e. during shutdown/teardown, not during gameplay — so they are not
evidence of systemic Blaze3D breakage while rendering. A broad "make modern Blaze3D run natively on
ancient hardware" compatibility layer was explicitly deferred on that basis, pending one clean report
proving the narrow fixes work.

**Still unconfirmed, and this is the whole point of v1.8**: neither targeted fix has *ever* been observed
working on real hardware. `VerifyMixinsHarness` proves the bytecode transforms apply (18/18 against the
real jar); the unit tests prove the transforms are correct in isolation. Whether they actually fix
invisible water / black lava / the invisible end portal remains the outstanding acceptance test.
