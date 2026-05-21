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
// auto-patch it from the compile-target version (which would otherwise stamp
// since-build="252" and cause the Marketplace to reject the upload on newer IDEs).
// since-build is wide (243 = IntelliJ Platform 2024.3+); until-build = "999.*"
// keeps the plugin installable on any future IDE version without manual bumps.
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild  = "243"
            untilBuild  = "999.*"
        }
    }
}
