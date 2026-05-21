# Changelog

## [0.12.0] - 2026-05-21
### Added
- **Tag autocomplete**. Typing `#` and any letters in a markdown file, then pressing `Ctrl+Space` (or waiting for auto-popup), now suggests every existing `#tag` from the current document. Selecting a suggestion completes the rest of the tag in place.
- Tag completion respects the same detection rules as the highlighter — a `#` must be at the start of the line or follow whitespace, and the first character after `#` must be a letter.
- Pure `extractAllTags(text)` function in new `TagCompletion.kt`, with 8 new JUnit tests covering deduplication, hyphen/underscore tags, multi-line documents, and rejection of issue numbers like `#123`.

## [0.11.0] - 2026-05-21
### Added
- **Colour customisation UI**. Chevron Lists' seven highlight colours are now exposed under `Settings → Editor → Color Scheme → Chevron Lists`. Each entry shows a live preview of what the colour controls:
  - Header chevron (`>`)
  - Item chevrons (`>>`, `>>>`, ...)
  - Tag (`#urgent`, `#blocked`)
  - Priority high (`!!!`)
  - Priority medium (`!!`)
  - Priority low (`!`)
  - Due date (`@YYYY-MM-DD`)
- Demo text in the settings page renders a realistic standup-notes example so users can see exactly how their colour choices will look in real markdown files.
- Colours respect the active editor colour scheme by default (Darcula, Light, High Contrast, etc.) and inherit appropriately from semantic IntelliJ defaults (METADATA, KEYWORD, NUMBER, STRING, etc.).

## [0.10.0] - 2026-05-21
### Added
- **`CL: Promote Item`** — decreases the current item's chevron depth (`>>> - foo` → `>> - foo`). No-op at depth 2 since that's the minimum for an item.
- **`CL: Demote Item`** — increases the current item's chevron depth (`>> - foo` → `>>> - foo`). No upper bound; items can be nested as deep as you want.
- **`CL: Cycle List Type`** — toggles the current item between bullet and numbered form (`>> - foo` ↔ `>> 1. foo`). Auto-fix-numbering will then renumber any resulting sequence.
- All three available via the editor right-click menu and `Ctrl+Shift+A` action search. Multi-line selection processes every item in the selection in one stroke. Markers and content (`⭐`, `#tags`, `@dates`, etc.) are fully preserved through the transformation.
- Pure `computePromote`, `computeDemote`, `computeCycleListType` functions in new `ItemTransforms.kt`, with 22 new JUnit tests covering depth bounds, content preservation, number/prefix handling, and non-item rejection.

### Changed
- Extracted abstract `ChevronLineTransformAction` base class so each new line-transform action is a one-line subclass that delegates to a pure compute function. Future actions (e.g. cycle bullet prefix, normalise spacing) add zero boilerplate.

## [0.9.0] - 2026-05-21
### Added
- **Inline syntax highlighting** — three new visual decorations applied to any markdown file (chevron items and plain paragraphs alike):
  - **`#tags`** like `#urgent`, `#blocked`, `#in-progress` are highlighted using your IDE's metadata colour. Must start with a letter and may contain word characters and hyphens.
  - **Priority markers** as standalone tokens:
    - **`!!!`** (high) — KEYWORD colour
    - **`!!`**  (medium) — NUMBER colour
    - **`!`**   (low) — PREDEFINED_SYMBOL colour
  - **Due dates** like `@2026-04-22` (ISO format) are highlighted using your STRING colour.
- All five new `TextAttributesKey` constants (`CHEVRON_LISTS_TAG`, `..._PRIORITY_HIGH/MEDIUM/LOW`, `..._DATE`) are user-customisable via `Settings → Editor → Color Scheme → General`.
- New pure `InlinePatterns.kt` module with `findTags`, `findPriorities`, `findDueDates` and `PriorityMatch` data class.
- 22 new JUnit tests covering tag detection (word-boundary rules, allowed character sets, multi-tag lines), priority detection (level recognition, greedy capping at 3, standalone-token rules), and date format strictness.

## [0.8.1] - 2026-05-21
### Fixed
- Plugin compatibility range was being auto-patched by the Gradle plugin to `since-build="252"` based on the compile target, which caused the JetBrains Marketplace to reject the upload as "Not compatible with the version of your running IDE (WebStorm 2026.1.2)". The compile target is now `intellijIdea("2026.1")` and the range is explicitly controlled via `intellijPlatform.pluginConfiguration.ideaVersion` (`since-build="243"`, `until-build` open).

## [0.8.0] - 2026-05-21
### Added
- **Plugin icon** — a native SVG reproduction of the VS Code plugin's logo (four chevron rows in purple/lime/blue/lavender on a `#12122A` background). Scales crisply from 16×16 in the plugin manager to 80×80+ on the Marketplace listing.
- **Marketplace-ready description** — detailed feature list, code example, and link to the VS Code counterpart, rendered in the JetBrains Marketplace listing.
- **Change notes** — versioned history surfaced in the plugin manager's "What's New" tab.
- **Compatibility range** — `since-build="243"` (IntelliJ Platform 2024.3+); `until-build` left open so new IDE releases install without manual bumps.

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