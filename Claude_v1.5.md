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
