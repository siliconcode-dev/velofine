# CLAUDE.md — Velofine

This file is guidance for Claude Code when working in this repository. Read `Masterdoc.md` for full project context and `Build_plan.md` for the phased implementation plan before starting work. Always know which phase you're currently in.

## Project summary

Velofine is a standalone, launcher-level OptiFine-style patcher for Minecraft Java Edition 26.2, targeting Windows only. It is not a Fabric/NeoForge mod. It patches the game via a `-javaagent` at the bytecode level, installs as a custom launcher profile, and ships as a single branded `.exe` installer. Three independent engines: **LegacySupport** (ancient-hardware compatibility, especially a documented Intel HD 4000 rendering bug), **Optimus** (performance, OpenGL-focused), and **Utility** (OptiFine-parity QoL features + full shader pipeline support).

## How to work in this repo

- **Follow the phase you're in.** `Build_plan.md` is authoritative for sequencing. Don't build Phase 4 features while Phase 1's exit criteria are unmet. If a phase's exit criteria can't be verified (e.g. no access to the reference legacy hardware), say so explicitly rather than marking it done.
- **Phase 1 (patcher pipeline) and Phase 2 (Intel HD 4000 fix) are the highest-stakes phases.** Phase 1 is the architectural bet the whole project rests on. Phase 2 is a real, personally-verified bug affecting a real person (the project owner's friend) — treat correctness here as more important than speed.
- **No access to reference legacy hardware.** The project owner does not personally own the i3-3110M / Intel HD 4000 test machine — validation of LegacySupport fixes depends on a friend/community tester. When implementing LegacySupport features, write clear manual test/verification steps (what to check in the F3 debug screen, what log output confirms the fix engaged, etc.) so a non-developer tester can confirm results.
- **Keep engines decoupled from the start**, even before the toggle UI exists (that's Phase 5). LegacySupport, Optimus, and Utility should be structured as independently buildable/runnable modules from Phase 1 onward.
- **Never let a feature silently degrade vanilla fidelity.** This applies especially to LegacySupport's memory-saving work (Phase 3) and Utility's texture/CTM features — "vanilla+" means vanilla behavior stays intact unless the user has opted into a change.

## Tech stack & conventions

- **Build tool:** Gradle, single monorepo with modules: `core` (shared utilities: config system, logging, GPU/hardware detection), `legacysupport`, `optimus`, `utility`, plus installer/packaging tooling.
- **Language/runtime:** Java, targeting Java 25 (Minecraft 26.1+'s requirement). Confirm the exact minimum Java version Velofine itself requires vs. what it bundles for end users — these can differ.
- **Patching approach:** Runtime bytecode transformation via `-javaagent`, Mixin-style. Research and pick a concrete Mixin/ASM tooling choice early in Phase 1 and document the decision in this file once made — don't leave it implicit in code.
- **Installer packaging:** `jpackage` (JDK-native, bundles a JRE) → **Inno Setup** for the final Windows `.exe`. UI direction: **black / white / red palette, brutalist design language** — apply this consistently across installer screens and any in-game Velofine UI (config panels, benchmark overlay), not just the installer.
- **License:** LGPL for the whole project. Apply LGPL license headers consistently. If/when Phase 7 (shaders) adapts components from Iris (also LGPL), keep attribution and licensing clean — do not silently vendor code without proper LGPL compliance (source availability, license notices).
- **No telemetry, ever.** No crash reporting, no analytics, opt-in or otherwise. Do not add any network call that phones home data about the user's system beyond the explicitly-designed in-app updater checking for new releases.
- **No code signing for v1.** Windows SmartScreen warnings on the unsigned installer/updater are expected and accepted — don't try to work around this with anything other than clear README/installer messaging.

## Testing

- Manual QA is acceptable through Phase 1–8. Don't block early phases on test coverage.
- Starting Phase 9, add automated unit/integration tests for the now-stabilized core: patcher pipeline, each engine's core logic, config system.
- CI (build pipeline) should exist from Phase 0 onward regardless of test coverage maturity — every push should at least compile and package successfully.

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

## Open decisions to resolve during implementation (not blockers to starting)

- Exact OpenGL context/profile LegacySupport will force on Intel HD 4000-class hardware (Phase 2 research task).
- Whether 26.2's bundled LWJGL version independently contributes to the legacy-hardware rendering bug (Phase 2 research task).
- Whether to build the shader-format parser from scratch or adapt Iris's LGPL components (Phase 7 decision point).
- Whether to ship an automatic crash-recovery "safe mode" (Phase 9 revisit).

When any of these get resolved, update `Masterdoc.md`'s "Open Questions" section and this file so future work (and future Claude Code sessions) stay in sync with the decision.
