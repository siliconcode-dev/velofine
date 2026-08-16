# Velofine 1.5 — Master Specification (Update Release)

## 0. Context: This Is an Update, Not a New Build

**Velofine v1 has already shipped.** This document, its companion `Build_plan_v1.5.md`, and `Claude_v1.5.md` are a fresh, self-contained spec for the **v1.5 update** — they intentionally do not continue v1's old phase numbering. Claude Code should treat this as extending the existing shipped codebase (especially the existing LegacySupport module), not rebuilding from scratch.

**Version label for this release: `Velofine 1.5 MC 26.2`**, per the project's existing `Velofine P<patch> MC <version>` scheme.

## 1. What v1.5 Is For

v1's LegacySupport Engine shipped without a real fix for the flagship bug it was designed around: on ancient Intel-iGPU hardware, Minecraft 26.2 renders **portals, lava, and water as fully invisible (x-ray-through)**, and some blocks render **fully black**. v1.5 exists to actually ship that fix, based on new founder-led research into the root cause.

**v1.5 scope is deliberately narrow:** LegacySupport fixes + the diagnostic/update tooling needed to support them. **Optimus and Utility are untouched in v1.5** — broader Optimus performance work is explicitly deferred to v2.

## 2. Root Cause (Founder Research)

Between Minecraft 1.16.5 and current versions, Mojang replaced the legacy fixed-function OpenGL pipeline with a modern, programmable, data-driven shader pipeline:

- **1.17:** Introduced required **Core Shaders** (GLSL 150) and raised the minimum graphics API to **OpenGL 3.2 Core**. Confirmed: rendering on the target hardware was fine/native on 1.16.5 and began breaking gradually on versions after it — this is the inflection point.
- **Post-1.19.4:** Mojang restructured uniforms, vertex attributes, and render passes further. Render programs like `rendertype_solid`, `rendertype_translucent`, `rendertype_cutout`, and `rendertype_end_portal` now pass significant data via uniform buffer objects and complex vertex arrays. (Confirmed independently: Minecraft's core shaders are real, inspectable GLSL files shipped at `assets/minecraft/shaders/core/` inside the client jar — replaceable the same way resource packs already override them — and the shader system does use genuine uniform blocks, not just loose uniforms.)

**Failure mechanism (hypothesis, not yet instrument-confirmed):**
- **Invisible lava/water/portals:** These render via `rendertype_translucent` / `rendertype_end_portal`, which pass fluid animation, lightmap, and texture-coordinate data through buffer/array structures the legacy Intel driver may fail to bind or process correctly. The fragment shader either discards the pixel (→ invisible/x-ray) or falls back to an unlit black output (→ solid black), depending on exactly how the driver mishandles it.
- **Fully black blocks:** `rendertype_solid` / `rendertype_cutout` rely on vertex color attributes (biome tinting, smooth lighting) combined with ambient occlusion uniforms. If the legacy driver fails to interpolate vertex colors correctly against Mojang's modern lighting math, the effective lighting value can resolve to zero, multiplying the texture color by zero → solid black.

**Important nuance:** the target Intel iGPUs report full driver-level support for the OpenGL versions Minecraft requires (HD 4000 reports OpenGL 4.0 on Windows, well above the 3.2 floor). This is not a "hardware/driver too old to comply" problem — it's specific, real bugs in how the driver implements specific modern shader/buffer features it otherwise claims to support. This is corroborated by an active, still-open Intel Community thread documenting a related class of bug on Intel iGPUs affecting Sodium/Iris (including one confirmed fix: a `vec3 mix()` light-color calculation bug with a known GLSL-level workaround), and by an open Sodium GitHub issue about Intel driver-version detection.

**Practical implication for the fix strategy:** because the driver claims support for the required GL version, forcing a *different/lower* GL context is unlikely to help — the fix needs to be targeted shader/buffer-binding workarounds for the specific broken paths, not context downgrading.

## 3. Target Hardware for v1.5

Two distinct GPUs, not two drivers on the same chip — treat them as separate signature-registry entries:

| Machine | CPU | iGPU | Driver | EUs |
|---|---|---|---|---|
| Reference machine A | i3-3110M | Intel HD Graphics **4000** | Intel-provided driver, v15.33.53.5161 (10/23/2020 — the last driver Intel shipped for this GPU generation) | 16 |
| Reference machine B | i5-3470S | Intel HD Graphics **2500** | Windows-provided (Windows Update) driver | 6, single texture sampler |

Both are personally verified/testable via a friend and testers. **Detection should extend beyond these two exact models to the broader Ivy Bridge-era Intel HD 2500/4000 family**, since this class of bug is architecture-driven, not model-specific — but unverified matches get a more conservative fix (see §5).

Confirmed test conditions: bug reproduces identically on a fresh vanilla 26.2 install and on Fabric 26.2 + Sodium + Lithium + other performance mods — ruling out a mod-conflict explanation and confirming it's a driver/GL-compliance-class issue.

## 4. Fix Architecture

- **Detection layer (hybrid):**
  - Pre-context OS-level detection (WMI/DXGI on Windows) runs early enough to pre-select the "Experimental Legacy Fix" toggle at install time (the installer can ask "does this describe your PC?").
  - Post-context confirmation via `glGetString(GL_VENDOR / GL_RENDERER / GL_VERSION)` once a real GL context exists, as the actual gate before any shader swap is applied — this protects against hybrid/switchable-graphics laptops where WMI and the active render context disagree.
  - Follows a **Sodium-style pattern**: a GPU/driver adapter probe feeding a signature registry of known-bad hardware, each mapped to a specific workaround — applied pre-context-creation where possible, extended with the post-context confirmation above.
- **Fix delivery:** Corrected core shaders are shipped as **swappable asset files** (patched `.fsh`/`.vsh` replacing the shipped `rendertype_*` shaders for matched hardware) rather than compiled into the agent — this lets a fix be updated via a small asset patch later without a full app rebuild. Java-side GL-call interception is a fallback only, reserved for cases where the bug turns out to live in buffer setup rather than the shader itself.
- **General shader-robustness improvements** (applied globally, regardless of detected hardware — low risk, broad benefit): defensive fallback values for uniforms/buffers instead of assuming correct binding; avoiding dynamic (non-constant) array indexing in fragment shaders (a known weak point for older Intel shader compilers); universal `GL_DEBUG_OUTPUT`/`KHR_debug` enablement so real driver-reported errors are always available, not just on flagged hardware.
- **Opt-in, not automatic:** the whole fix ships behind an **"Experimental Legacy Fix"** toggle — offered both as an install-time suggestion (if detected hardware matches) and as an in-game settings toggle a user can find and enable themselves. It is explicitly not auto-enabled by default in v1.5, given development is happening blind (no direct hardware access) and this is genuinely original, unvalidated-until-tested work. May move to opt-out-by-default for matched hardware in a future release once battle-tested.
- **Confidence-tiered fix strength:**
  - **Verified models (i3-3110M/HD 4000, i5-3470S/HD 2500) exact driver match:** full targeted shader-patch fix.
  - **Unverified but matched broader HD 2500/4000-family hardware:** a more cautious variant — the compatibility-renderer fallback only, no shader patch — until more field reports come in.

## 5. Diagnostic Tooling — `diagnostic.exe`

Since development has no direct access to the affected hardware and telemetry is explicitly off, v1.5 includes a **standalone, portable diagnostic tool** — not background data collection, and not built into Velofine's own UI:

- **Standalone tool, not a Velofine feature.** `diagnostic.exe` is a separate, portable, single-file executable with its own simple GUI (no install step — same model as tools like GPU-Z/HWiNFO64). It lives in the Velofine monorepo and ships bundled as an extra file alongside the main Velofine installer in the same GitHub release, but **must never depend on Velofine being installed.**
- **Requires Minecraft, not Velofine.** On first run it asks the user to point it at their Minecraft installation directory. It needs this to pull the *real* shipped `rendertype_*` shader source from the client jar — it does not need Velofine present at all.
- **Real shader compilation, not just system info.** It creates its own minimal OpenGL context (LWJGL, `GL.createCapabilities()` + `glCompileShader`/`GL_COMPILE_STATUS`/`GL_INFO_LOG_LENGTH`) and actually attempts to compile the extracted `rendertype_translucent`, `rendertype_end_portal`, `rendertype_solid`, and `rendertype_cutout` shaders against the user's real driver — capturing genuine pass/fail plus the driver's own error text, not just "here's your GPU model."
- **Dual mode:** in addition to producing a baseline diagnosis, it can also accept a folder of candidate *fixed* shader files (sent by the founder during Phase 4 iteration) and re-run the same compile-and-report check against them — turning it into the actual fix-testing feedback loop, not just a one-time snapshot tool.
- **Output:** timestamped JSON report (e.g. `velofine-diagnosis-2026-08-15-1420.json`) — timestamped so repeated runs across fix attempts don't overwrite each other and can be compared before/after.
- **Versioning:** `diagnostic.exe` has its own independent version number, separate from Velofine's own version — it will likely be updated multiple times across Phase 4 fix attempts before v1.5 itself ships.
- **License:** same LGPL as the rest of the project, public in the `silicon-dev/velofine` repo.
- **Delivery flow stays informal for v1.5:** the tool saves its report locally; the tester hands the file to the founder, who relays it to Claude Code for the actual fix work. No GitHub issue template or automated submission pipeline yet — that's a "later, once public" concern, not a v1.5 requirement.

## 6. Update & Rollout

- v1.5 pushes to **all existing v1 users automatically** via the in-app auto-updater once ready — the *update itself* is not gated as an opt-in beta (only the in-game Experimental Legacy Fix feature is opt-in).
- Updater safety pattern: **download → validate → atomic replace via a staged temp path**, plus **snapshot the previous version and auto-restore it if the new build fails to launch** (this is the established safe pattern other Minecraft auto-updaters use, and Velofine has no code-signing chain to lean on instead, so this matters more than usual).
- Rollback trigger: **automatic** rollback only for hard failures (crash on launch, fails to start) — that's an unambiguous, safe thing to automate. Softer regressions (a user notices a new visual glitch after updating) are handled via a **manual one-click "revert to previous version"** button plus the diagnostic tool, not auto-detected.
- A lightweight **changelog / "What's New in 1.5"** is included — cheap to produce, and given no telemetry and no code signing, it's one of the few trust signals users get that an update is intentional and legitimate.

## 7. Non-Goals for v1.5

- No Optimus or Utility changes (Optimus broadening is v2).
- No public GitHub issue template / automated diagnostic submission pipeline yet (testers-first, informal).
- No opt-out-by-default fix activation yet (stays opt-in for this release).
- No code signing, no telemetry — same v1 constraints still apply.

## 8. Definition of Done

**v1.5 is complete when the two verified reference machines (i3-3110M/HD 4000 and i5-3470S/HD 2500) render lava, water, portals, and previously-black blocks correctly** under the Experimental Legacy Fix toggle, with no regressions versus the earlier Fabric+Sodium+Lithium baseline test. Broader HD 2500/4000-family compatibility (beyond the two verified machines) is a bonus, not a blocker, for calling v1.5 done.
