# Current Iteration

## Iteration Goal

Improve code quality, harden the AI workflow rules, and align human-facing documentation with the AI memory system.

## Completed In This Iteration

- Reviewed the current codebase and identified high-priority implementation issues.
- Fixed `PrinterUtil` so bundled import resources are optional rather than a hard failure condition.
- Fixed `PrinterUtil.printPdf` so blank printer names no longer trigger a null-pointer path and print failures surface as service-level errors.
- Restored recursive temporary-directory cleanup in the HTML print flow.
- Improved `/api/printPDF` validation and response flow so it participates more cleanly in the shared API handling model.
- Strengthened `AGENTS.md` to require workflow recording and synchronized maintenance of `docs/ai/`, `README.md`, `README.zh-CN.md`, and `readme/`.
- Rewrote the human-facing English and Chinese README entry documents.
- Added `readme/` documentation for API reference, usage guidance, packaging guidance, and documentation navigation.
- Expanded `readme/` into an English-and-Chinese paired documentation set.
- Added the `human-rules/` sanctuary and recorded that human-authored Markdown rules override AI-generated repository rules.
- Reworked single-instance startup control to use local file-lock ownership, removing dependency on startup signature or local control ports.
- Changed web-port conflict handling so the UI remains open and shows an English message instead of treating the conflict as duplicate startup.
- Changed UI readiness transition so `Status: Ready !` is set only after Vert.x HTTP listen succeeds; if startup fails, UI now returns to `Status: Stopped` and keeps web port input editable.

## Current Product Understanding

- The product remains a desktop-oriented local print bridge for trusted environments.
- Human-facing documentation is now explicitly split between entry README files and detailed guides under `readme/`.
- The repository now expects AI workflow memory and external documentation to evolve together.
- The repository now also has a human-authored rule space that AI must read but not modify.
- The desktop application is now intended to run as a single local instance per host user session, enforced by local file-lock ownership rather than process-signature matching.

## Immediate Risks Or Gaps

- External endpoints and logging token configuration remain hard-coded in source.
- Linux native packaging is not yet defined as a dedicated Maven packaging profile.
- There is still no formal example set for advanced HTML asset strategies beyond the API guide.

## Recommended Next Iteration Focus

- Review whether version-check and remote error-reporting endpoints should become configurable.
- Decide whether to add Linux native packaging support and document it.
- Consider documenting cluster-based print instruction exchange if Vert.x cluster mode becomes a real feature.
