# Publishing Chevron Lists (JetBrains)

Releases are uploaded **by hand** to the JetBrains Marketplace. There is no
`PUBLISH_TOKEN` secret, so the Release workflow builds the plugin and attaches the
zip to the GitHub release but does not publish.

## Release checklist

1. On a branch: bump `version` in `gradle.properties` and add a
   `## [X.Y.Z] - date` section to `CHANGELOG.md`. This repo never keeps an
   `[Unreleased]` section. PR, green CI, squash-merge.
2. Build from an up-to-date `main`. The build pins `jvmToolchain(23)`, so point
   `JAVA_HOME` at a JDK 23 first; under JDK 17 `instrumentCode` fails with
   "...\Packages does not exist".

   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-23.0.2.7-hotspot'
   .\gradlew.bat buildPlugin
   ```

3. Check `build/distributions/ChevronListsJetBrains-X.Y.Z.zip`: `plugin.xml` in
   the jar must show `<version>X.Y.Z</version>` and **no `until-build`**.
4. Upload at <https://plugins.jetbrains.com/plugin/31877-chevron-lists/edit/versions>
   (*Upload Update*, Stable channel). JetBrains reviews it, which takes up to two
   business days.
5. Every push to `main` recreates a draft GitHub release for the current version.
   Once the `main` build has finished, publish it:
   `gh release edit X.Y.Z --draft=false --latest`. That creates the `X.Y.Z` tag
   (no `v` prefix) and the Release workflow attaches the zip.

## Keep `until-build` open

A fixed `until-build` hides the plugin from every IDE newer than the cap, which
silently delists it for those users. `build.gradle.kts` sets
`untilBuild = provider { null }`; the Marketplace rejects placeholders such as
`999.*`. Check compatibility with:

```
POST https://plugins.jetbrains.com/api/search/compatibleUpdates
{"build": "IU-<build>", "pluginXMLIds": ["com.lewisisworking.chevronlists"]}
```

## Automated publishing, if a token is ever added

JetBrains Marketplace tokens are free (<https://plugins.jetbrains.com/author/me/tokens>).
Store one with `gh secret set PUBLISH_TOKEN --repo LewisIsWorking/ChevronListsJetBrains`
(`gh` prompts for the value; never paste it anywhere else) and the Release workflow
will run `publishPlugin` when a GitHub release is published.
