# Velofine

A standalone, launcher-level OptiFine-style patcher for Minecraft Java Edition 26.2. Not a Fabric/NeoForge mod — Velofine installs as its own custom launcher profile and patches the game at the bytecode level to deliver dramatically better performance, legacy-hardware compatibility, and a curated set of quality-of-life visual features on top of vanilla.

Three independent engines:
- **LegacySupport** — fixes for ancient/low-end hardware (e.g. Intel HD 4000-class GPUs).
- **Optimus** — OpenGL-focused performance optimization.
- **Utility** — OptiFine-parity QoL features and full shader pipeline support.

**Status:** Early scaffolding (Phase 0 of the build plan). Not yet usable.

## License

Velofine is licensed under the [GNU Lesser General Public License v3.0](LICENSE).

## Building

Requires JDK 25.

```sh
./gradlew build
```

---

Full install instructions and screenshots land in Phase 1, once the patcher pipeline and installer exist.
