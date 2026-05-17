# Changelog

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