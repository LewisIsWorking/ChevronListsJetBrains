# Changelog

## [0.7.0] - 2026-05-21
### Added
- **`CL: Toggle Star`**  — toggles a ⭐ marker on the current item.
- **`CL: Toggle Pin`**   — toggles a 📌 marker.
- **`CL: Toggle Flag`**  — toggles a 🚩 marker.
- **`CL: Toggle Note`**  — toggles a 📝 marker.
- All four available via the editor right-click menu and `Ctrl+Shift+A` action search. Markers can coexist: starring a flagged item yields `⭐ 🚩 Task` rather than replacing the flag. Multi-line selection toggles every item in the range in one stroke.
- Pure `toggleMarker(content, marker)` and `computeToggleMarker(line, prefix, marker)` functions in `ItemCommands.kt`, with 17 new JUnit tests covering presence detection, removal at any position, marker coexistence, and edge cases (empty content, multiple internal spaces, depth/number preservation).

### Changed
- Extracted abstract `ChevronMarkerToggleAction` base class so each new marker action is a one-line subclass. Future markers (e.g. priority, colour label) add zero boilerplate.

## [0.6.0] - 2026-05-17
### Added
- **`CL: Toggle Done`** action (`Ctrl+Alt+D`). Cycles the current chevron item between unchecked and checked:
  - `>> - Task` → `>> - [x] Task`
  - `>> - [x] Task` → `>> - [ ] Task`
  - Works on bullet items (`>> -`) and numbered items (`>> 1.`)
  - Works at any chevron depth (`>>`, `>>>`, `>>>>`)
- Multi-line support: select multiple lines and `Ctrl+Alt+D` toggles every chevron item in the selection in one stroke. Non-item lines (headers, plain markdown) are silently skipped.
- Action available under the editor right-click menu as well as the shortcut.
- Pure `computeToggleDone` and `toggleCheckbox` functions in `ItemCommands.kt` with full plain-JUnit coverage (20 tests).

## [0.5.0] - 2026-05-17
### Added
- **Auto-fix numbering as you type**. The plugin now watches all open `.md` files and silently renumbers numbered lists when sequences break. Typing `>> 2.` immediately after `>> 1. ... >> 2.` (a duplicate) automatically becomes `>> 3.`. Independent per section and per chevron depth — lists in different sections never collide.
- New `autoFixNumbering` setting under `Tools → Chevron Lists`, on by default. Toggle off if you want only the warning underlines without auto-edits.
- Pure `computeAutoFixEdits` function in `Diagnostics.kt` with full plain-JUnit coverage (7 new tests).
- Application-level `ChevronAutoFixListener` service with 250ms debounce, applied via `WriteCommandAction` so edits stack with the user's own undo history correctly.

### Changed
- The "Default new list type" dropdown now shows **Numbered list (>> 1.)** / **Bullet list (>> -)** for clarity. The underlying stored values remain `"ordered"` / `"unordered"` for cross-plugin consistency with the VS Code extension.

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