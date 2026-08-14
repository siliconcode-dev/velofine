# Velofine — Build Plan

Phased implementation plan. Each phase should be functionally complete and validated before moving to the next. Do not skip ahead to later-phase features while an earlier phase's core goal is unvalidated — this is a "vanilla+" tool for real users on real broken hardware, and an unstable foundation undermines the whole premise.

---

## Phase 0 — Project Scaffolding
**Goal:** A buildable, empty skeleton that proves the toolchain works.

- Initialize the Gradle monorepo with modules for `legacysupport`, `optimus`, `utility`, plus a shared `core`/`common` module for cross-engine utilities (config system, logging, GPU/hardware detection).
- Set up CI (build pipeline) from day one — compile + package on every push, even with no real logic yet.
- Establish LGPL license headers/boilerplate across the repo.
- Set up the repo's public GitHub Projects board / roadmap.
- Write a minimal README (full README with screenshots comes in Phase 1, but a placeholder stub belongs here).

**Exit criteria:** `./gradlew build` succeeds; CI is green; empty modules exist with correct package structure.

---

## Phase 1 — Minimal Patcher Pipeline (end-to-end validation)
**Goal:** Prove the core distribution mechanism works before building any actual features. This is the highest-risk architectural bet in the whole project — validate it first.

- Build the thin wrapper launcher jar that self-attaches a `-javaagent` and hands off to Minecraft's real main class.
- Build the custom launcher profile generator (custom version JSON pointing at Velofine's patched libraries/agent jar) that installs alongside a user's existing vanilla 26.2 installation.
- The agent at this stage does **nothing functional** yet — it should just prove it successfully loads, attaches, and lets vanilla Minecraft 26.2 boot normally, unmodified.
- Build the installer (`jpackage` → Inno Setup pipeline): single `.exe`, `.minecraft`/game-directory picker, real-time install progress. Apply the black/white/red brutalist UI direction here, even at v1 polish level — this is the first thing every user sees.
- Ship unsigned; add a clear, non-alarming note in the installer and README about the expected Windows SmartScreen warning.
- Write the real Phase-1 README: what Velofine is, install instructions, screenshots/GIFs of the installer.

**Exit criteria:** A user can run the Velofine installer, pick their game directory, launch a "Velofine" profile from the vanilla Minecraft Launcher, and reach the title screen of unmodified-behavior vanilla 26.2 — with the agent confirmed attached (e.g. via a log line or debug marker).

---

## Phase 2 — LegacySupport: the Intel HD 4000 fix
**Goal:** Ship the founder's proven, personally-verified bug fix. This is the flagship first real feature, chosen deliberately ahead of performance work.

- Research task: pin down the exact OpenGL context/profile Minecraft 26.2 requests by default vs. what Intel HD 4000 driver 15.33.53.5161 actually supports; identify whether the fix is context negotiation, a specific extension gap, or shader-level (or a combination).
- Research task: confirm/rule out whether 26.2's bundled LWJGL version is a contributing factor independent of the GL context issue.
- Implement GPU/driver auto-detection (identify Intel HD 4000-class hardware and this specific driver generation) and apply a pre-built fix profile automatically on match.
- Implement the actual fix: forced GL context negotiation and/or targeted GLSL shader patches for the affected draw calls (portal, lava, water rendering — the confirmed "fully invisible" bug).
- Implement the generic fallback "safe mode" / compatibility renderer for old/unknown GPUs that don't match the specific pre-built profile.
- Since there's no personal access to the reference hardware: build this phase with a tight feedback loop to the friend/community tester who has the affected machine — treat their confirmation as the actual acceptance test, not just code review.

**Exit criteria:** On the reference i3-3110M / Intel HD 4000 / driver 15.33.53.5161 machine, portals/lava/water render correctly (not invisible) under Velofine, on both a fresh install and confirmed non-regressing versus the earlier Fabric+Sodium+Lithium test case.

---

## Phase 3 — LegacySupport: broader hardware & memory
**Goal:** Round out LegacySupport beyond the single flagship bug.

- HDD/IO-related stutter mitigation (e.g. chunk load stall smoothing).
- RAM-saving features for 4GB-class systems (texture streaming, reduced entity/particle caps) — with an explicit non-negotiable constraint: vanilla look/behavior must not visibly degrade as a side effect.
- Generalize the auto-detect + fix-profile system built in Phase 2 so new hardware/driver profiles can be added without re-architecting.

**Exit criteria:** Demonstrable stutter reduction and lower memory footprint on the reference hardware class, with no vanilla-fidelity regressions.

---

## Phase 4 — Optimus Engine (core)
**Goal:** Performance optimization, OpenGL-focused, dual-core-aware.

- Chunk mesh caching and threaded/multithreaded chunk building, designed against a 2-core assumption as the floor (not 4+).
- Multithreaded world generation.
- Entity/AI tick throttling.
- CPU-side profiling to find and address pathfinding/tick bottlenecks specific to the low-end hardware profile.

**Exit criteria:** Measurable FPS/tick-time improvement on the reference hardware class vs. Phase-2/3 baseline, with no correctness regressions (no broken game logic from tick throttling).

---

## Phase 5 — Optimus Engine (adaptive) + per-engine toggling UI
**Goal:** Make performance behavior smart and user-controllable.

- Adaptive/auto performance governor: real-time FPS-based adjustment of render distance/graphics quality.
- Fixed manual mode as an alternative to the governor.
- Build the independent-toggle GUI panels for each engine (LegacySupport / Optimus / Utility), each with its own config panel, per the "not one combined settings screen" requirement.
- Wire up Utility's "safe-by-default on weak hardware" behavior against LegacySupport's hardware detection from Phase 2.

**Exit criteria:** A user can independently enable/disable each engine and see the governor visibly respond to FPS changes in real time.

---

## Phase 6 — Utility Engine: core QoL features
**Goal:** The OptiFine-parity feature set, excluding shaders (which get their own phase due to complexity).

- Zoom (smooth, scroll-adjustable).
- Dynamic Lights (held + dropped/entity-held sources).
- Connected Textures (OptiFine CTM format compatibility), Better Snow/Grass, Natural Textures.
- Variable Render Distance (separate horizontal/vertical).
- Fog, mipmaps, AF/AA, Vsync controls.
- Built-in benchmarking/FPS overlay tool.

**Exit criteria:** Each feature independently toggleable, visually correct, and not regressing performance work from Phase 4/5.

---

## Phase 7 — Utility Engine: shader pipeline
**Goal:** The most technically ambitious Utility feature — full shader support.

- Decision checkpoint (flagged in Masterdoc as open): build the OptiFine/Iris shader-format parser from scratch, or adapt Iris's existing LGPL-licensed components (license-compatible since Velofine is also LGPL). Resolve this at the start of this phase, not before — it may be informed by how the rest of the architecture has settled by this point.
- Support both OptiFine-format and Iris-format shader packs as drop-in files, no user-side code changes.
- Validate against a handful of popular real-world shader packs (BSL, Complementary, etc. — same ones Iris validates against) for compatibility baseline.
- Validate shader support doesn't break LegacySupport's Intel HD 4000 fixes (this hardware almost certainly cannot run most shader packs at playable framerates, but it shouldn't crash or corrupt rendering when shaders are toggled off again).

**Exit criteria:** A sample of popular OptiFine- and Iris-format shader packs load and render correctly with no code changes required from the user.

---

## Phase 8 — Auto-updater
**Goal:** In-app update flow.

- Check for new Velofine releases and apply updates in-app.
- Support selecting/patching against specific Minecraft versions (not just "always latest"), consistent with the `Velofine P<patch> MC <version>` versioning scheme.
- Since the app is unsigned, ensure the updater itself doesn't introduce additional trust/security red flags beyond the already-accepted SmartScreen warning (e.g. verify update package integrity even without a code-signing chain — checksums at minimum).

**Exit criteria:** A running Velofine install can detect, download, and apply a newer release without the user manually re-running the installer.

---

## Phase 9 — Polish, docs, and test coverage catch-up
**Goal:** Bring the project up to a genuine v1.0 release bar.

- Add automated unit/integration tests for the now-stabilized architecture (patcher pipeline, each engine's core logic) — this is the deferred point mentioned in the Masterdoc for when test investment should ramp up.
- Full README polish with complete screenshots/GIFs, updated for every feature shipped since Phase 1.
- Revisit the deferred `CONTRIBUTING.md` / code style guide now that the codebase has stabilized.
- Revisit the "maybe" items: auto-safe-mode crash recovery (relaunch with LegacySupport forced on after a startup crash), and whether a Discord/community channel should be stood up now.

**Exit criteria:** v1.0 tag-ready: installer, three engines, updater, docs, and a baseline test suite all present and working together.

---

## Notes for Claude Code on sequencing

- **Phase 1 is the highest-risk phase.** If the custom-launcher-profile + javaagent approach hits an unforeseen blocker, that's an architecture-level problem to flag back to the project owner before continuing — don't silently work around it with a materially different distribution mechanism.
- **Phase 2 (Intel HD 4000 fix) is the emotional and practical core of the project's v1 pitch.** Treat it with proportionate care — it's the one bug that's been personally verified and is actively affecting a real person.
- Engines should be built with their independent-toggle boundary in mind from Phase 1 onward, even though the actual toggle UI doesn't land until Phase 5 — retrofitting decoupling later is expensive.
