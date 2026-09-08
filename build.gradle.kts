import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
    // Coverage. Kover reports LINE and BRANCH natively for Kotlin, which is what
    // the 100%-coverage policy requires.
    id("org.jetbrains.kotlinx.kover")
}

// Pin one JVM toolchain for both compileJava and compileKotlin. The IntelliJ
// Platform Gradle Plugin derives the Kotlin jvmTarget from the target platform
// (23 for 2026.2), while Java otherwise follows whatever JDK runs Gradle -- a
// local JBR 25 produced "Inconsistent JVM-target compatibility (25 vs 23)".
// Fixing the toolchain keeps local and CI builds identical.
kotlin {
    jvmToolchain(23)
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // Compile against the current IDE release. The Plugin Verifier in CI
        // checks the whole declared range (243 through 263), which is what
        // catches APIs that exist here but not on older supported IDEs --
        // exactly how the EnterHandlerDelegate.postProcessEnter break surfaced.
        intellijIdea("2026.2.2")
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

    // `.github/workflows/release.yml` passes these as environment variables, but
    // the Gradle plugin does not pick them up on its own -- without this block
    // `publishPlugin` fails with a missing token even when the repository
    // secrets are set, so the automated release path had never been wired up.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey       = providers.environmentVariable("PRIVATE_KEY")
        password         = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

// ---------------------------------------------------------------- coverage
//
// Policy is 100% coverage with no exclusions. These bounds are a RATCHET
// towards it: `koverVerify` fails when coverage drops below the recorded floor,
// so the numbers can only go up. Raise them as tests land; never lower them.
//
// Baseline recorded 2026-09-07, the first measurement of this plugin -- there
// was no coverage tooling at all before. 28 of 58 classes were at 100% and the
// other 30 at 0%, and the zero-coverage set is exactly the IntelliJ-platform
// layer (annotator, actions, configurable, completion contributor, the
// document listener). The pure logic modules were already fully covered.
//
// Kover reports LINE and BRANCH natively, so unlike the VS Code repo -- where
// Bun emits no branch data and istanbul had to be bolted on -- both halves of
// the policy are measurable out of the box.
//
// Kover's CoverageUnit exposes LINE, BRANCH and INSTRUCTION only -- there is no
// METHOD unit, though the XML report does carry a METHOD counter (37.28% at
// baseline) if it is ever wanted for reporting.
//
// TARGET for every bound: 100.
val coverageFloorLine        = 42
val coverageFloorBranch      = 45
val coverageFloorInstruction = 50

kover {
    reports {
        total {
            verify {
                // One rule per metric so a failure names the metric that
                // regressed rather than just "a bound was violated".
                rule("line coverage must not regress") {
                    bound {
                        minValue = coverageFloorLine
                        coverageUnits = CoverageUnit.LINE
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
                rule("branch coverage must not regress") {
                    bound {
                        minValue = coverageFloorBranch
                        coverageUnits = CoverageUnit.BRANCH
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
                rule("instruction coverage must not regress") {
                    bound {
                        minValue = coverageFloorInstruction
                        coverageUnits = CoverageUnit.INSTRUCTION
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
    }
}
