# Changelog

## [0.4.0] - 2026-05-17
### Added
- **Settings panel**. New `Settings → Tools → Chevron Lists` panel with two configurable options:
  - **List prefix** — the character used after `>>` for bullet items (default `-`, change to `*` for `>> *`, etc.).
  - **Default new list type** — `unordered` inserts `>> - ` after Enter on a header (default), `ordered` inserts `>> 1. ` so headers start numbered lists by default.
- Persistent settings stored in `chevronLists.xml` at the IDE config level, shared across all projects.
- `ChevronEnterHandler` now reads both settings live — no IDE restart required when you change them.

## [0.3.0] - 2026-05-17
### Added
- **Enter-key list continuation**. Pressing Enter on a chevron line now auto-inserts the next item:
  - On `> Header` → inserts `>> - ` (first item of a new list)
  - On `>> - content` → inserts `>> - ` (next bullet)
  - On `>> N. content` → inserts `>> N+1. ` (next number)
  - On an empty list item → clears the line and falls through to default Enter (ends the list)
- Pure logic in `EnterContinuation.kt` with full plain-JUnit coverage (18 tests).

## [0.2.0] - 2026-05-17
### Added
- Bad-numbering diagnostic: flags numbered lists that break sequence (e.g. `>> 1.` followed by `>> 3.`) with a warning underline and a hover message showing the expected number. Independent per section and per chevron depth — lists in different sections never collide.
- Empty-section diagnostic: flags any `> Section` header that has no chevron items before the next section starts. The currently-trailing section is never flagged since you may still be writing it.
- New `Diagnostics.kt` module: pure diagnostic functions (`collectIssues`, `collectDuplicateHeaders`, `collectDuplicateSubheadings`, `collectBadNumbering`, `collectEmptySections`) with full plain-JUnit coverage.

### Changed
- `ChevronListsAnnotator` refactored to split syntax highlighting from diagnostic detection. All diagnostic logic now lives in `Diagnostics.kt` and the annotator simply translates pure findings into IntelliJ annotations.

## [0.1.0] - 2026-05-17
### Added
- Initial release: chevron syntax highlighting in markdown files
- Warning underline for duplicate `> Section` headers
- Warning underline for duplicate `## Subheading` markdown headings
- `CL: Open Settings` placeholder action under the Tools menu
- Pure parsing logic in `Patterns.kt` with full JUnit coverage