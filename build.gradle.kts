import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.2.6.2")
        testFramework(TestFrameworkType.Platform)
    }
}

// Explicitly control the plugin's compatibility range so the Gradle plugin doesn't
// auto-patch it from the compile-target version (which would cap the plugin at the
// 2025.2 build it compiles against).
//
// since-build = 243 (IntelliJ Platform 2024.3+) for broad backwards compatibility.
//
// until-build is deliberately OPEN. A fixed upper bound is a delisting trap: the
// Marketplace hides the plugin from every IDE newer than the cap, so the moment a
// new major ships the plugin silently disappears for those users until someone
// remembers to bump and re-release. v0.14.1 pinned "261.*", which would have hidden
// the plugin from all of 2026.2 (build 262) and 2026.3 (263). The published 0.8.0
// has no upper bound and stayed compatible throughout, which is the behaviour we want.
//
// `provider { null }` is the Gradle-plugin-supported way to omit the attribute
// entirely; the Marketplace rejects "magic" placeholders such as 999.*.
// This plugin uses only stable, long-standing extension points (annotator,
// enterHandlerDelegate, completion.contributor, applicationConfigurable,
// postStartupActivity, colorSettingsPage), so an open bound is low risk.
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild  = "243"
            untilBuild  = provider { null }
        }
    }
}
