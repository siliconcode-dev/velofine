# Contributing to Velofine

Thanks for considering a contribution. Velofine is a small, single-maintainer-driven project — this guide is intentionally short.

## Before you start

Read `Masterdoc.md` (full project context) and `Build_plan.md` (the phased implementation plan) — Velofine follows its build plan strictly; a PR that jumps ahead of the current phase, or reintroduces something explicitly marked out of scope in `Masterdoc.md` §8, is likely to be declined regardless of code quality. If you're unsure whether something fits, open an issue first rather than a PR.

## Development setup

Requires JDK 25. Windows is the only supported build/runtime target for v1 (see `Masterdoc.md`); the Gradle build itself compiles fine on any OS, but the installer (`:installer:innoSetupCompile`) and the `legacysupport` live-jar test only run on Windows.

```sh
./gradlew build
```

This compiles every module, runs the unit test suite, and checks formatting/license headers. Common tasks:

```sh
./gradlew test                # run all unit tests
./gradlew spotlessApply       # auto-fix formatting + missing/stale license headers
./gradlew jacocoTestReport    # generate coverage reports (informational, no enforced gate)
```

`legacysupport`'s `VerifyMixinsHarness` additionally runs live against a real vanilla 26.2 client jar when one is available (it's proprietary Mojang code, so it can't be committed here — the test skips cleanly without it):

```sh
./gradlew :legacysupport:test -Pvelofine.test.mcJarPath=<path to a real 26.2.jar>
```

## Code style

- Formatting and LGPL license headers are enforced by Spotless (`./gradlew spotlessCheck`, part of `check`/`build`). Run `spotlessApply` before committing rather than hand-formatting.
- Follow the patterns already established in the module you're touching before introducing a new one — this codebase has strong, deliberate conventions (see `CLAUDE.md`) around things like Mixin usage, config-section shape, and cross-engine dependency direction. When in doubt, match the nearest existing example.
- Prefer small, explicit test seams (a package-visible overload, a `resetForTest()` hook) over reaching for a mocking framework when a class wasn't designed with dependency injection in mind — see `core.updater.SignatureVerifier`/`GitHubReleaseClient` or `core.crash.CrashRecovery` for the pattern this project actually uses.
- No comments explaining *what* code does — only *why*, when it's non-obvious (a hidden constraint, a workaround for a specific confirmed bug, a subtle invariant).

## Tests

Starting Phase 9, new code touching `core`/each engine's own logic (not GUI code, not `@Mixin` classes, not hardware-detection shell-outs — see `core/src/test/java` for what's realistically unit-testable in this codebase and why) should come with real JUnit 5 tests, not just manual verification. `Build_plan.md`'s Testing section and the existing `src/test/java` trees are the reference for scope and style.

## Pull requests

1. Keep PRs scoped to one phase/feature at a time.
2. `./gradlew build` must pass locally before opening a PR — CI runs the same check.
3. Explain the *why* in your PR description, not just the *what* — this project's commit history and `CLAUDE.md` are written the same way, and reviewers will expect it.
4. By contributing, you agree your changes are licensed under the project's LGPL-3.0 license (see `LICENSE`).

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). Participation implies agreement to it.

## Questions

Open a [GitHub Issue](https://github.com/siliconcode-dev/velofine/issues) — there's no separate community chat for this project yet.
