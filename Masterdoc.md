# Velofine — Master Specification

## 1. What Velofine Is

Velofine is a **standalone, launcher-level OptiFine-style patcher** for Minecraft Java Edition **26.2 (Chaos Cubed)**. It is not a Fabric or NeoForge mod — it installs as its own custom launcher profile (the way OptiFine did pre-1.13) and patches the game at a deep, root code level to deliver a "vanilla+" experience: the same game, dramatically better performance and compatibility, plus a curated set of quality-of-life visual features.

Installing Velofine means going **Velofine-only** — it is not designed to coexist with Fabric/NeoForge or other mods/loaders. It is a full replacement path for players who currently choose between "play modded for performance/shaders" and "play vanilla because it's simple."

- **Project name:** Velofine
- **Repo:** `silicon-dev/velofine` (GitHub)
- **License:** LGPL (open source)
- **Target Minecraft version:** 26.2 (Java Edition), with an in-app updater intended to support patching against other/older Minecraft versions over time
- **Target OS for v1:** Windows only
- **Target Java:** Java 25 (required by MC 26.1+); Velofine bundles/recommends a specific Java 25 distribution rather than relying on whatever JRE the user has

## 2. Why It Exists

Two problems, one tool:

1. **Ancient hardware is badly broken on modern Minecraft.** Since Minecraft 1.17 raised its minimum requirement to an OpenGL 3.2 Core context, older Ivy Bridge-era Intel integrated GPUs (HD 4000/HD 2500-class — e.g. an i3-3110M laptop with 4GB DDR3 and an HDD, or an i5-3470S desktop with 4GB DDR3 and an SSD) have suffered escalating rendering bugs. On 26.2 specifically, this manifests as **fully invisible (x-ray-through) portals, lava, and water**, plus poor overall performance. This is confirmed to happen on both fresh vanilla 26.2 and on Fabric 26.2 with Sodium/Lithium installed — it is not a mod conflict, it's a driver/GL-compliance-class bug. This class of bug is independently documented (Intel Community forum threads, an open Sodium GitHub issue about Intel driver detection) and Intel has not shipped a new driver for this hardware generation since October 2020 (driver 15.33.53.5161) — so the fix has to live in software, not "update your drivers."
2. **OptiFine's role has fragmented.** OptiFine is closed-source, slow to update, and conflicts with performance mods. The community answer (Sodium + Iris on Fabric/NeoForge) is excellent but requires a mod loader and doesn't help the legacy-hardware case above. Velofine aims to be a single-install, no-mod-loader-required tool that covers both: modern performance techniques *and* legacy-hardware compatibility fixes, in one product.

## 3. Architecture

- **Type:** Standalone launcher-level patcher, not a mod-loader mod.
- **Patching mechanism:** Runtime bytecode transformation via a `-javaagent`, Mixin-style (like Sodium/Iris/Lithium's approach, applied standalone rather than through Fabric). The agent is embedded inside a thin wrapper launcher jar that a custom Velofine launcher profile points to; the wrapper attaches the agent, then hands off to Minecraft's real main class.
- **Distribution model:** A custom launcher profile (custom version JSON pointing at Velofine's patched libraries/agent jar), installed alongside the user's existing vanilla 26.2 installation — the same conceptual approach OptiFine used pre-1.13.
- **Repo structure:** Single monorepo, Gradle build, with LegacySupport / Optimus / Utility as independent modules/engines.
- **Engine independence:** Each engine is independently toggleable at runtime via its own GUI config panel (not one combined settings screen). A user could, in principle, run Utility's cosmetic features without Optimus's aggressive optimizations, or vice versa.
- **No coexistence goal:** Velofine is not designed to run alongside other mods or mod loaders. It must, however, work seamlessly as a client-side tool on **vanilla multiplayer servers**.
- **Versioning scheme:** `Velofine P<patch semver> MC <Minecraft version>`, e.g. `Velofine 2.1 MC 26.2` — tracks Velofine's own version alongside the Minecraft version it targets.

### Known Minecraft 26.2 rendering context (for reference)
- 26.2 ships an experimental, opt-in **Vulkan renderer** alongside OpenGL, but it defaults to OpenGL and Vulkan is preferred for dedicated GPUs, not integrated ones. Old hardware stays on OpenGL regardless. **Optimus stays OpenGL-focused** — no Vulkan integration planned.
- Minecraft has required OpenGL 3.2 Core since 1.17. This is the version boundary where legacy-Intel-iGPU rendering started breaking.

## 4. The Three Engines

### 4.1 LegacySupport Engine — "make it work on ancient hardware"
**Target hardware floor (v1):** two reference machines, both Ivy Bridge-generation Intel with the same driver-support cutoff era:
- Intel Core i3-3110M / 4GB DDR3 / HDD, Intel HD 4000 integrated graphics, Windows, driver 15.33.53.5161 (Intel's last driver for this GPU generation, Oct 2020) — dual-core laptop profile.
- Intel Core i5-3470S / 4GB DDR3 / SSD, Intel HD Graphics 2500 integrated graphics, Windows — quad-core desktop profile, weaker/different iGPU (HD 2500 vs. HD 4000) but a stronger CPU; same driver-era GL-compliance risk as the i3-3110M.

This is the *initial* floor; future versions may target even lower-spec hardware.

Confirmed symptoms on this class of hardware (26.2):
- Portals, lava, and water render **fully invisible** (see-through, not solid black) — an x-ray-like effect.
- General poor performance.
- Reproduces identically on fresh vanilla 26.2 and on Fabric 26.2 + Sodium/Lithium — confirming it's a driver/GL-compliance bug, not a mod conflict.
- Confirmed fine/native on 1.16.5; breaks gradually starting with the versions after 1.16.5 (i.e., coincides with the 1.17 OpenGL 3.2 Core requirement change).

Scope:
- Force a specific/hybrid OpenGL context (exact profile/version TBD — needs technical research in Phase 1/2) to work around the driver's compliance gaps, rather than relying on the user to update drivers (Intel will not ship new ones for this hardware).
- Targeted GLSL shader patches/fallbacks for the specific draw calls known to break on old Intel/AMD drivers (scoped fixes, not a full pipeline rewrite) — informed by prior art like the Intel Community thread documenting a `vec3 mix()` light-color bug and its GLSL-level fix.
- Auto-detect this GPU/driver class and apply a pre-built fix profile automatically; fall back to a generic "safe mode" for other unknown/old GPUs.
- Non-GPU legacy bottlenecks too: HDD/IO-related stutter (e.g. chunk load stalls), not rendering-only.
- RAM-saving features for 4GB-class systems (texture streaming, reduced entity/particle caps) — but vanilla look/behavior must stay solid and unchanged; LegacySupport should never visibly compromise vanilla fidelity to save memory.
- A fallback **"compatibility renderer" mode** — lower fidelity, maximum compatibility, last-resort — similar in spirit to OptiFine's old "Fast" graphics mode, for hardware LegacySupport can't fully fix.
- This is the **first concrete feature to be built**, ahead of Optimus/core performance work, since it's the founder's personally-verified, proven bug (a friend is actively affected by it).

### 4.2 Optimus Engine — "make it fast"
Pure performance/optimization engine, OpenGL-focused (not Vulkan).

Scope:
- Sodium/Lithium-equivalent techniques: chunk mesh caching, threaded/multithreaded chunk building and world generation, entity/AI tick throttling.
- Multithreading designed with dual-core hardware in mind (the i3-3110M is dual-core) — must not assume 4+ cores are available.
- Both CPU-side (chunk building, entity ticking, pathfinding) and GPU-side/rendering optimization.
- An adaptive/auto **performance governor**: real-time FPS-based adjustment of render distance/quality settings, in addition to a fixed manual mode.

### 4.3 Utility Engine — "make it nicer, beyond vanilla"
Extra features beyond vanilla, each independently toggleable:

- **Zoom** — smooth, scroll-adjustable zoom (Zoomify-style), not a fixed single zoom level.
- **Dynamic Lights** — responds to both held light sources (player) and dropped/entity-held light sources (e.g. a mob holding a torch).
- **Connected Textures** — OptiFine CTM resource-pack format supported for backward compatibility with existing texture packs.
- **Better Snow/Grass, Natural Textures** — as per OptiFine's traditional feature set.
- **Variable Render Distance** — separate horizontal and vertical render distance controls (not natively offered by vanilla Mojang settings).
- **Fog, mipmaps, AF/AA, Vsync** — standard OptiFine-equivalent video settings.
- **Full custom shader pipeline support** for both **OptiFine-format** and **Iris-format** shader packs — drop-in files, no code changes required from the user. (Architecture note: Iris's own shader-compatibility code is LGPL-3.0; since Velofine is also LGPL, this is a green light to build on or adapt Iris's approach/components directly rather than reimplementing from scratch — see Build_plan Phase notes.)
- **Built-in benchmarking/FPS overlay tooling** — a lightweight debug/benchmark overlay to help users tune settings for their specific hardware.
- **Safe-by-default scaling:** Utility features should be automatically disabled or reduced when LegacySupport detects weak/flagged hardware, rather than defaulting to "on" everywhere.

## 5. Installer & Distribution

- **Format:** A single polished `.exe` installer for Windows.
- **Build approach:** `jpackage` (JDK 14+, bundles its own JRE) to produce the app image, wrapped with **Inno Setup** (free, actively maintained) for the final branded installer — directory picker, real-time progress, uninstall support.
- **UI/UX direction:** High-end, "stunning" visual polish. **Black / white / red palette, brutalist UI design language.** Includes a `.minecraft` / game-directory picker and a real install-progress indicator.
- **Code signing:** None for v1 — ship unsigned and accept that Windows SmartScreen/Defender will show an "unrecognized publisher" warning on first run. This is a known, expected trade-off for unsigned indie/open-source Windows software, not a sign of malware; document it clearly in the README so users aren't alarmed.
- **Updates:** In-app auto-updater. Should support not just "update to newest," but also selecting/patching against **older Minecraft versions**, since Velofine's own versioning ties to specific MC versions.
- **Telemetry:** **None.** No crash reporting, no analytics, opt-in or otherwise. This was explicitly rejected.

## 6. Open Source / Community

- License: **LGPL**.
- Public roadmap / GitHub Projects board: yes, from day one.
- README with screenshots/GIFs and install instructions: yes, from Phase 1 onward.
- `CONTRIBUTING.md` / code style guide: deferred — add once the codebase stabilizes, not in early phases.
- Discord/community feedback channel: maybe, later — not a day-one requirement.

## 7. Testing

- No personal access to the reference legacy hardware during development — real-hardware validation depends on two testers with the i3-3110M and i5-3470S reference machines respectively, and, later, community testers.
- Manual QA is acceptable for early phases; automated unit/integration tests should be added once the core architecture (patcher pipeline, engine boundaries) stabilizes rather than from the very first line of code.
- CI (build + test pipeline) is wanted from day one regardless, even while test coverage itself ramps up gradually.

## 8. Explicit Non-Goals (v1)

- No Fabric/NeoForge/other-mod coexistence.
- No macOS/Linux support.
- No Vulkan rendering path in Optimus.
- No telemetry/analytics/crash reporting.
- No code signing / no paid certificate.
- No `CONTRIBUTING.md` or heavy community infrastructure yet.

## 9. Open Questions / Not Yet Decided

These should be resolved during early implementation phases, not blocking project kickoff:

- OpenGL context/profile LegacySupport forces is now implemented (Phase 2): vanilla requests 3.3 Core Forward-Compatible (confirmed via javap against the real client jar); on detected Intel Gen7 hardware, Velofine forces 3.3 Compatibility Profile instead. This is a research-grounded hypothesis, not yet confirmed against the actual reference hardware — see CLAUDE.md's "LegacySupport: real findings" section.
- 26.2's bundled LWJGL version is confirmed 3.4.1 (via its own version JSON); whether it independently contributes to the bug beyond the GL-context/shader issues already addressed is still open.
- Whether to build the OptiFine/Iris shader-format parser from scratch or adapt Iris's existing LGPL components.
- Whether Velofine should ship an automatic crash-recovery "safe mode" (auto-relaunch with LegacySupport forced on after a startup crash) — leaning yes, not committed.
- Phase 3's RAM-saving/IO-smoothing fixes are implemented (mipmap/render/simulation/entity-distance/particle first-run defaults via `OptionsMixin`; eager chunk-save throttling via `ChunkMapMixin`), both research-grounded against the real 26.2 client jar the same way Phase 2's GL fix was, but **not yet confirmed** to reduce stutter/memory footprint on the actual i3-3110M/i5-3470S reference hardware — see CLAUDE.md's "LegacySupport: real findings" section for the Phase 3 addendum.
- True texture-atlas streaming (dynamic load/evict, not just a lower mipmap-level default) was explicitly scoped out of Phase 3 as too risky to build without reference-hardware validation — left as a future revisit if the mipmap-only lever proves insufficient.
