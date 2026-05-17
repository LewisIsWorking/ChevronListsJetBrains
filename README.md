# Chevron Lists for JetBrains IDEs

A JetBrains port of the [Chevron Lists VS Code extension](https://marketplace.visualstudio.com/items?itemName=lewisisworking.chevron-lists) — a markdown-based task and project management plugin using `>`, `>>`, `>>>` blockquote nesting.

## Status

**v0.1.0 — initial release.** Minimum viable feature set:
- Syntax highlighting for chevron lines (`>`, `>>`, `>>>`) in markdown files
- Warning underline for duplicate `> Section` headers
- Warning underline for duplicate `## Subheading` markdown headings
- `CL: Open Settings` action (placeholder for now, under the Tools menu)

## Roadmap

Features to be ported from the VS Code extension, roughly in priority order:
1. Bad-numbering diagnostics (sequence breaks in `>> 1.`, `>> 2.` lists)
2. Empty-section diagnostics
3. Auto-fix numbering on type
4. Enter-key list continuation
5. Item-level commands (toggle done, move up/down, etc.)
6. Settings panel with real-time controls
7. Command launcher
8. AI assist integration
9. Daily notes, templates, kanban, statistics

## Development

Requires JDK 21+ (the bundled JBR from any JetBrains IDE works). Build with:

```powershell
$env:JAVA_HOME = "C:\Users\Lewis\AppData\Local\Programs\Rider\jbr"
.\gradlew build
```

Run a sandbox IDE with the plugin loaded:

```powershell
.\gradlew runIde
```

## Repository conventions

- All source files ≤ 200 lines (extract via SOLID/OOP, never trim)
- Pure logic separated from IntelliJ Platform code for plain-JUnit testing
- 100% test pass rate before every push
- No suppressed warnings or compiler errors