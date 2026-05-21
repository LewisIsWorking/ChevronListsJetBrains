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
// auto-patch it from the compile-target version. since-build = 243 (IntelliJ Platform
// 2024.3+) for broad backwards compatibility. until-build = 261.* matches the latest
// available IDE version at release time (WebStorm/IntelliJ 2026.1.x); the Marketplace
// rejects "magic" placeholder values like 999.* and recommends setting this to the
// actual latest IDE major. Bump this when a new IDE major (262/263/...) ships.
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild  = "243"
            untilBuild  = "261.*"
        }
    }
}
