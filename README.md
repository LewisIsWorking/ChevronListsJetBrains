# Chevron Lists for JetBrains IDEs

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/31877?label=JetBrains%20Marketplace)](https://plugins.jetbrains.com/plugin/31877-chevron-lists)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/31877)](https://plugins.jetbrains.com/plugin/31877-chevron-lists)

**[Get from Marketplace](https://plugins.jetbrains.com/plugin/31877-chevron-lists)**, or in the IDE: Settings > Plugins > Marketplace, search "Chevron Lists".

A JetBrains port of the [Chevron Lists VS Code extension](https://marketplace.visualstudio.com/items?itemName=lewisisworking.chevron-lists) - a markdown-based task and project management plugin using `>`, `>>`, `>>>` blockquote nesting.

## Features

Current version: **0.17.0**. See [CHANGELOG.md](CHANGELOG.md) for the history.

**In the editor**
- Syntax highlighting for chevron lines (`>`, `>>`, `>>>`), with colours set under Settings > Editor > Color Scheme
- Warnings for duplicate `> Section` headers, duplicate `## Subheading` headings, numbering breaks and empty sections
- Numbering fixed as you type, and Enter continues the list
- Sections fold, showing the header and item count
- Pasting several lines into an item makes each line its own item
- `#tag` completion

**Actions** (all start with `CL:`, so Find Action lists them together)
- Items: Toggle Done, Star, Pin, Flag or Note; Promote and Demote; Move Up and Down; Duplicate; Cycle List Type; Set or Remove Colour Label
- Sections: Mark All Done or Undone, Archive Done Items, Renumber Items, Sort A to Z or Z to A, Convert Bullets to Numbered and back
- Navigation: Go to Section, Filter by Tag, Jump to Next or Previous Header
- Daily notes: Open Daily Note (Tools menu), Send to Daily Note
- Templates: Insert Template, Save Section as Template; Import, Export and Delete (Tools menu)
- Sets: tag a heading `#set` to warn when an item is in two of its lists; Move Item to List
- Open Settings

## Roadmap

Still to port from the VS Code extension:
1. AI assist integration
2. Kanban view
3. Statistics

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