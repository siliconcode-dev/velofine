# Velofine

A standalone, launcher-level OptiFine-style patcher for Minecraft Java Edition 26.2. Not a Fabric/NeoForge mod — Velofine installs as its own custom launcher profile and patches the game at the bytecode level to deliver dramatically better performance, legacy-hardware compatibility, and a curated set of quality-of-life visual features on top of vanilla.

Three independent engines, each with its own on/off toggle and in-game config panel:
- **LegacySupport** — fixes for ancient/low-end hardware (Intel HD 4000 / HD Graphics 2500-class GPUs).
- **Optimus** — OpenGL-focused performance optimization, including an adaptive FPS-based render-distance governor.
- **Utility** — OptiFine-parity QoL features and full OptiFine/Iris-format shader pack support.

**Status:** `1.5.0-Beta`. v1 (all nine build phases — see `Build_plan_v1.5.md`'s history section for the original phased plan) shipped without a confirmed fix for the flagship Intel Gen7 rendering bug; v1.5 adds targeted shader patches plus a standalone `diagnostic.exe` tool, gated behind an opt-in "Experimental Legacy Fix" toggle. Several LegacySupport/Optimus findings are research-grounded and bytecode-verified but **not yet confirmed on the actual reference hardware** (an Intel HD 4000 laptop and an HD Graphics 2500 desktop) — that's pending community testers; see `CLAUDE.md` for exactly which claims are and aren't confirmed.

## Features

### LegacySupport

- Auto-detects Intel Gen7-class GPUs (HD 4000 / HD 2500) and old/unknown GPUs generally, forcing an OpenGL 3.3 Compatibility Profile context and a defensive GLSL `mix()` patch — the fix candidates for the invisible-portals/lava/water bug on this hardware class.
- IO-stall smoothing for rotational (HDD) game-directory drives, and lowered first-run video-setting defaults for 4GB-class systems.
- Every fix is independently overridable (Auto/On/Off) from the in-game panel.
- **Safe mode**: if the game fails to reach a healthy running state on two consecutive launches, LegacySupport's GL-compatibility fixes are forced on for that one launch only — transparent, logged, and automatically reverts once a session runs cleanly. Never a silent or permanent change.

### Optimus

- Explicit background-thread-pool sizing, an AI goal-selector re-evaluation throttle, and a lightweight tick-time profiler.
- An adaptive performance governor: real-time FPS-based render-distance adjustment (descend fast, climb slow, never above your own setting), or a fixed manual mode.

### Utility

- Smooth, scroll-adjustable zoom; fog control; anti-aliasing (FXAA); variable (separate horizontal/vertical) render distance; dynamic lights from held items; an FPS/frame-time readout.
- Full OptiFine- and Iris-format shader pack support — drop packs into `shaderpacks/`, select one from the in-game panel. (v1 targets packs authored against vanilla 26.2's own modern GLSL naming convention — see `CLAUDE.md`'s Phase 7 notes for the compatibility-scope decision behind this.)

### Updates

Velofine checks for new releases automatically (once every ~24h, metadata only — no telemetry, ever) and on demand from the **UPDATES** panel. A new release is downloaded and verified (SHA-256 checksum **and** an Ed25519 signature) before you're ever offered the option to install it — unsigned software still deserves real integrity checks.

## Installing

1. Make sure vanilla Minecraft 26.2 is already installed and has been launched at least once through your Minecraft Launcher (official or a standard-format third-party launcher — Velofine reads and extends the same `versions/` + `launcher_profiles.json` files they use).
2. Download `Velofine-Setup-<version>.exe` from the [latest release](https://github.com/siliconcode-dev/velofine/releases) and run it.
3. **Windows will likely show a "Windows protected your PC" SmartScreen warning.** This is expected: Velofine is unsigned, open-source software, and a paid code-signing certificate is explicitly out of scope for v1 (see `Masterdoc.md` §5). It is not a sign of malware — click "More info" → "Run anyway" if you're comfortable proceeding, or review the source first since this is all open source under LGPL.
4. Pick your `.minecraft` folder when prompted (auto-detected at `%APPDATA%\.minecraft` if present).
5. Once installed, open your Minecraft Launcher and select the new **Velofine** profile.

Velofine installs to `%LocalAppData%\Velofine` — no admin rights needed. Uninstalling removes the generated Velofine version entry and profile alongside the app itself; your vanilla installation is never modified.

Once in-game, open Velofine's config screen from the Video Settings menu, the pause menu, or a rebindable keybind (unbound by default) — one panel per engine, plus General and Updates.

## Building from source

Requires JDK 25.

```sh
./gradlew build
```

This compiles every module, runs the real unit test suite (JUnit 5 — see `core`, `optimus`, `utility`, `shaders`, and `launcher`'s `src/test/java` trees), and checks license headers/formatting (Spotless). `legacysupport`'s bytecode-transform diagnostic (`VerifyMixinsHarness`) additionally runs live against a real vanilla 26.2 client jar when one is available:

```sh
./gradlew :legacysupport:test -Pvelofine.test.mcJarPath=<path to a real 26.2.jar>
```

— it skips cleanly otherwise (the client jar is proprietary and can't be committed to this repo). Coverage reports (JaCoCo, informational — no enforced threshold) land at `<module>/build/reports/jacoco/test/html/index.html`.

To build the Windows installer (Windows only, requires [Inno Setup 6](https://jrsoftware.org/isinfo.php) installed):

```sh
./gradlew :installer:jpackageAppImage :installer:innoSetupCompile
```

Produces `installer/build/innosetup/Velofine-Setup.exe`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for dev setup, the test/style-check workflow, and the PR process. This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). Bug reports and feature discussion happen on [GitHub Issues](https://github.com/siliconcode-dev/velofine/issues) — no separate community chat exists yet.

## License

Velofine is licensed under the [GNU Lesser General Public License v3.0](LICENSE).

---

Screenshots/GIFs of the installer and in-game config panels land once there's a GUI session available to capture them from — not blocking for this phase.
