# reference

`report.ReferenceBaseline` loads a "known-good" `diagnostic.exe` BASELINE report from
`velofine-reference-26.2.json` in this directory, if present, and every run's Results screen diffs
against it automatically via `report.ReportComparator` — this is what turns "compiled/linked/drew
without a crash" into an actual "does this look like normal hardware" verdict.

**This file does not exist yet and must be captured manually, once, on real non-Intel-Gen7
hardware** (this bug is architecture-specific — any modern GPU, including this project's own dev
machines, qualifies as "known-good" for this purpose):

1. Run `diagnostic.exe` in BASELINE mode against a real vanilla 26.2 install on normal hardware.
2. Copy the resulting `velofine-diagnosis-<timestamp>.json` here and rename it to
   `velofine-reference-26.2.json`.
3. Rebuild `diagnostics-tool` so the file is picked up as a bundled resource.

Until this file is added, `ReferenceBaseline.load()` returns empty and every run's Results screen
simply skips the reference diff — it does not fail or warn, since a fresh checkout with no reference
captured yet is an expected, not broken, state.
