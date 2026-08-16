# Velofine 1.5 — Build Plan (Update Release)

Fresh, self-contained phase plan for the v1.5 update. Phase numbers here restart at 1 and are scoped to this release only — they do not continue v1's original build plan. Claude Code should extend the existing shipped v1 LegacySupport module in place rather than rewriting it.

---

## Phase 1 — `diagnostic.exe` (build this first, before any fix code)
**Goal:** Get real diagnostic data flowing before writing a single line of fix logic. This directly de-risks "blind" development.

- Build `diagnostic.exe` as a **standalone module in the monorepo**, independent of Velofine itself — it must run and work correctly even on a machine that has never installed Velofine.
- Portable single-file Windows `.exe`, no install step, simple GUI (not console). Own independent version number, separate from Velofine's.
- On first run, prompts the user for their Minecraft installation directory (does not need Velofine's directory — only Minecraft's, to read the real client jar).
- Extracts the real `rendertype_solid`, `rendertype_translucent`, `rendertype_cutout`, and `rendertype_end_portal` shaders directly from the client jar (`assets/minecraft/shaders/core/`, plain GLSL, no decompiling needed).
- Creates its own minimal OpenGL context (LWJGL `GL.createCapabilities()`) and actually attempts to compile those real shaders, capturing genuine `GL_COMPILE_STATUS` + `GL_INFO_LOG_LENGTH` driver output — not just static GPU/driver metadata.
- Also captures: GPU vendor/model/driver version, OpenGL version/extension list, general GL error output (`GL_DEBUG_OUTPUT`/`KHR_debug`).
- **Dual mode:** also accepts a folder of candidate *fixed* shader files (to be sent by the founder later, during Phase 4) and re-runs the same compile-and-report check against them — this is what turns it into the actual fix-testing loop for Phase 4, not just a one-off tool, so build this mode now even though it isn't used until later.
- Output: timestamped JSON report (e.g. `velofine-diagnosis-2026-08-15-1420.json`) so repeated runs across fix attempts don't overwrite each other.
- Bundle it as an extra file in the same GitHub release/download as the main Velofine installer. Same LGPL license, public in the repo (e.g. a `/diagnostics-tool` module).
- Get a build into the current tester's (the founder's friend's) hands immediately to capture a **baseline report of the current broken state**, before any fix work starts. This baseline is the acceptance input for later phases.

**Exit criteria:** A working, standalone `diagnostic.exe` exists, runs without Velofine installed, and has produced at least one real baseline report from actual affected hardware (i3-3110M/HD 4000 first, since that's the original verified machine).

---

## Phase 2 — Detection Layer
**Goal:** Reliably identify the target hardware before deciding whether to apply any fix.

- Implement pre-context OS-level (WMI/DXGI on Windows) GPU detection, run early enough to pre-select the "Experimental Legacy Fix" toggle at install time.
- Implement post-context confirmation via `glGetString(GL_VENDOR/GL_RENDERER/GL_VERSION)` once a real GL context exists — this is the actual gate before any shader swap is applied, protecting against hybrid/switchable-graphics mismatches.
- Build the signature registry (Sodium-`Workarounds`-style): entries for the two verified machines (i3-3110M/HD 4000 + Intel driver 15.33.53.5161; i5-3470S/HD 2500 + Windows-provided driver) as exact-match, high-confidence entries, plus broader Ivy Bridge-era HD 2500/4000-family entries as lower-confidence matches.
- Wire confidence tier into fix selection: exact-verified match → eligible for full targeted fix (Phase 4); broader family match → eligible only for the conservative compatibility-renderer fallback (Phase 3).

**Exit criteria:** Given the Phase 1 baseline report's GPU/driver info, the detection layer correctly classifies it as a verified exact match.

---

## Phase 3 — Compatibility Renderer Fallback
**Goal:** Ship the safe, generic fallback first — this is lower-risk than the targeted shader patch and covers the broader unverified hardware family immediately.

- Implement/extend the "compatibility renderer" mode referenced in the v1 Masterdoc (lower fidelity, maximum compatibility) as the fallback fix for hardware matched at lower confidence.
- Apply the general shader-robustness improvements globally (regardless of detected hardware): defensive uniform/buffer fallback values, avoiding dynamic array indexing in fragment shaders, universal debug output (already done in Phase 1).

**Exit criteria:** Compatibility renderer mode is selectable via the Experimental Legacy Fix toggle and doesn't regress anything on hardware that doesn't need it.

---

## Phase 4 — Targeted Shader Fix (the core of v1.5)
**Goal:** The actual fix for the flagship bug, for the two verified machines.

- Extract and inspect the real shipped `rendertype_translucent.fsh`/`.vsh`, `rendertype_end_portal.fsh`/`.vsh`, `rendertype_solid.fsh`/`.vsh`, and `rendertype_cutout.fsh`/`.vsh` from the 26.2 client jar (directly readable, no decompiling needed — they're plain GLSL under `assets/minecraft/shaders/core/`).
- Using the Phase 1 baseline diagnostic report (driver-reported GL errors, shader compile/link status) as ground truth, identify the actual failing binding/uniform/attribute path per the root-cause hypothesis in the Masterdoc.
- Write corrected shader variants as **swappable asset files** (not compiled into the agent) targeting the confirmed failure points — informed by the known prior-art fix pattern (e.g. the documented `vec3 mix()` light-color workaround from the Intel Community/Sodium-adjacent research).
- Register these corrected shaders in the signature registry as the fix for exact-verified-match hardware.
- Iterate with the tester using `diagnostic.exe`'s fix-testing mode: send candidate corrected shader files, tester drops them into the tool and re-runs it, sends back the new timestamped report showing whether the candidate now compiles cleanly — much faster than shipping a full Velofine build each iteration. Confirm with an actual in-game visual check only once compilation succeeds. This is the phase most dependent on the tester feedback loop, since development itself is blind.

**Exit criteria:** On the i3-3110M/HD 4000 reference machine, with the Experimental Legacy Fix toggle enabled, portals/lava/water render correctly (not invisible) and previously-black blocks render correctly. Repeat and confirm on the i5-3470S/HD 2500 machine.

---

## Phase 5 — Updater & Rollback Safety
**Goal:** Ship this safely to the existing v1 user base.

- Implement the auto-updater delivery: download → validate → atomic replace via a staged temp path.
- Implement version snapshotting: keep the previous version available, auto-restore it if the newly-updated build fails to launch (hard-failure case only).
- Implement the manual one-click "revert to previous version" UI action for softer regressions a user notices post-update (not auto-triggered).
- Write and ship a short "What's New in 1.5" changelog, surfaced in-app and as GitHub release notes.
- Confirm the release is versioned and labeled **"Velofine 1.5 MC 26.2"**.

**Exit criteria:** A v1 install can receive, apply, and (if needed) roll back the v1.5 update without manual reinstallation.

---

## Phase 6 — Rollout
**Goal:** Get v1.5 into real hands, in order.

- Push to current testers first (the founder's friend, plus any additional testers already in the loop) — no public GitHub issue template or automated submission pipeline yet, this stays informal.
- Once the two verified machines confirm correct rendering (the core Definition of Done from the Masterdoc), push the update to the full existing v1 user base via the auto-updater.
- Broader HD 2500/4000-family confirmation from the wider user base is welcome feedback afterward, but is not a blocker for calling v1.5 done.

**Exit criteria:** v1.5 is live for all existing v1 users, with the two verified machines confirmed fixed.

---

## Notes for Claude Code

- **This is an extension, not a rewrite.** Work within the existing v1 LegacySupport module structure. If the new detection/registry system genuinely doesn't fit the existing code cleanly, flag that back rather than silently restructuring broadly.
- **Phase 1 (`diagnostic.exe`) unblocks everything else.** Development is blind without it — don't skip ahead to shader fixes without a real baseline report in hand. Remember it's a standalone tool, not a Velofine feature — keep it fully decoupled from the main app's codebase and install state.
- **Phase 4 is the emotionally and practically core phase.** This is original, previously-undocumented fix work for a real bug affecting a real person. Treat the tester feedback loop as the actual source of truth, more than any theoretical reasoning about the shader source.
- **v1.5 does not touch Optimus or Utility.** Resist scope creep into those engines — that's v2.
