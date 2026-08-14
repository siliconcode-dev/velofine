# Velofine

A standalone, launcher-level OptiFine-style patcher for Minecraft Java Edition 26.2. Not a Fabric/NeoForge mod — Velofine installs as its own custom launcher profile and patches the game at the bytecode level to deliver dramatically better performance, legacy-hardware compatibility, and a curated set of quality-of-life visual features on top of vanilla.

Three independent engines:
- **LegacySupport** — fixes for ancient/low-end hardware (Intel HD 4000 / HD Graphics 2500-class GPUs).
- **Optimus** — OpenGL-focused performance optimization.
- **Utility** — OptiFine-parity QoL features and full shader pipeline support.

**Status:** Phase 1 of the build plan — the patcher pipeline and installer exist, but none of the three engines do any real work yet (the agent attaches and does nothing). Not yet a useful mod. See `Build_plan.md` for what's next.

## Installing

1. Make sure vanilla Minecraft 26.2 is already installed and has been launched at least once through your Minecraft Launcher (official or a standard-format third-party launcher — Velofine reads and extends the same `versions/` + `launcher_profiles.json` files they use).
2. Download `Velofine-Setup.exe` and run it.
3. **Windows will likely show a "Windows protected your PC" SmartScreen warning.** This is expected: Velofine is unsigned, open-source software, and a paid code-signing certificate is explicitly out of scope for v1 (see `Masterdoc.md` §5). It is not a sign of malware — click "More info" → "Run anyway" if you're comfortable proceeding, or review the source first since this is all open source under LGPL.
4. Pick your `.minecraft` folder when prompted (auto-detected at `%APPDATA%\.minecraft` if present).
5. Once installed, open your Minecraft Launcher and select the new **Velofine** profile.

Velofine installs to `%LocalAppData%\Velofine` — no admin rights needed. Uninstalling removes the generated Velofine version entry and profile alongside the app itself; your vanilla installation is never modified.

## Building from source

Requires JDK 25.

```sh
./gradlew build
```

To build the Windows installer (Windows only, requires [Inno Setup 6](https://jrsoftware.org/isinfo.php) installed):

```sh
./gradlew :installer:jpackageAppImage :installer:innoSetupCompile
```

Produces `installer/build/innosetup/Velofine-Setup.exe`.

## License

Velofine is licensed under the [GNU Lesser General Public License v3.0](LICENSE).

---

Screenshots/GIFs of the installer land once there's a GUI session available to capture them from — not blocking for this phase.
