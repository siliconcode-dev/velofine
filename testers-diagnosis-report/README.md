# testers-diagnosis-report

Drop tester-submitted `diagnostic.exe` reports here (the timestamped
`velofine-diagnosis-YYYY-MM-DD-HHMM.json` files a tester's copy of the tool writes next to itself).

This folder is the intake point referenced by `Build_plan_v1.5.md` Phase 1's "informal delivery
flow": a tester runs `diagnostic.exe`, hands the JSON file back (however - email, Discord, USB
stick), and it gets dropped here. Later v1.5 phases (Phase 2 detection registry, Phase 4 shader-fix
iteration) read directly from this folder as real, ground-truth hardware data - keep every report
that comes in, including repeat runs from the same machine across fix iterations, since Phase 4's
loop depends on diffing a before/after pair for the same tester.

Suggested naming when a report doesn't already carry enough context in its own filename: prefix with
the tester/machine it came from, e.g. `friend-i3-3110m-hd4000-velofine-diagnosis-2026-08-16-1420.json`.

No automatic submission pipeline exists yet (and won't for v1.5 - see Masterdoc_v1.5.md S7) - every
file here was placed manually.
