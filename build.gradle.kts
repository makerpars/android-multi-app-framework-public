import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.room) apply false
    // ── Quality Tools ──
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover)

}

// Test JVM paralelliğini de sınırla
val cores = Runtime.getRuntime().availableProcessors()
tasks.withType<Test>().configureEach {
    maxParallelForks = (cores / 3).coerceAtLeast(1)
}

val qualityCheckTask = tasks.register("qualityCheck") {
    group = "verification"
    description = "Minimal checks: Android Lint + Detekt (deprecated only) + ktlint"
    dependsOn("detekt")
}

val pythonExecutable = providers.gradleProperty("pythonExecutable")
    .orElse(providers.environmentVariable("PYTHON"))
    .orElse(
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            "python"
        } else {
            "python3"
        },
    )

val validateAppAdsTxtTask = tasks.register<Exec>("validateAppAdsTxt") {
    group = "verification"
    description = "Validate app-ads.txt seller rows and required AdMob publisher line"
    commandLine(
        pythonExecutable.get(),
        "scripts/ci/validate_app_ads_txt.py",
        "--mode",
        "strict",
    )
}

qualityCheckTask.configure {
    dependsOn(validateAppAdsTxtTask)
}

// ═══════════════════════════════════════════════════════════════
// ▸ 1. Detekt — Minimal (Sadece Deprecated Detection)
// ═══════════════════════════════════════════════════════════════
detekt {
    source.setFrom(
        fileTree(rootDir) {
            include("app/src/**/*.kt", "core/*/src/**/*.kt", "feature/*/src/**/*.kt")
        })
    config.setFrom(files("config/detekt/detekt.yml"))
    baseline = file("config/detekt/detekt-baseline.xml")
    parallel = true
    buildUponDefaultConfig = false  // Sadece kendi config'imizi kullan
    autoCorrect = false
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        html.outputLocation.set(project.layout.buildDirectory.file("reports/detekt/detekt.html"))
    }
    jvmTarget = "21"
    // Build fail olmasın - sadece rapor -> ARTIK FAIL OLMALI
    ignoreFailures = false
}

// ═══════════════════════════════════════════════════════════════
// ▸ 2. Subproject quality config (Android Lint + ktlint)
// ═══════════════════════════════════════════════════════════════
subprojects {
    val preReleaseVersionRegex = Regex(""".*[-.](alpha|beta|rc)\d*.*""", RegexOption.IGNORE_CASE)
    val allowedPreReleaseGroups = setOf(
        // Google Mobile Ads 25.x transitively requires these beta AndroidX artifacts.
        "androidx.privacysandbox.ads"
    )
    val allowedPreReleaseModules = setOf(
        // Required transitively by androidx.credentials:credentials-play-services-auth:1.5.0.
        "com.google.android.gms:play-services-identity-credentials"
    )

    // Stabilize R8 inputs by pinning versions across the graph.
    val libsCatalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

    val coroutinesVersion = libsCatalog.findVersion("coroutines").get().requiredVersion
    val credentialsVersion = libsCatalog.findVersion("credentialsVersion").get().requiredVersion
    val googleIdVersion = libsCatalog.findVersion("googleidVersion").get().requiredVersion

    configurations.configureEach {
        resolutionStrategy {
            componentSelection {
                all {
                    val moduleCoordinate = "${candidate.group}:${candidate.module}"
                    val isAllowedPreRelease =
                        candidate.group in allowedPreReleaseGroups || moduleCoordinate in allowedPreReleaseModules
                    if (preReleaseVersionRegex.matches(candidate.version) && !isAllowedPreRelease) {
                        reject(
                            "Pre-release dependencies are not allowed: " + "${candidate.group}:${candidate.module}:${candidate.version}"
                        )
                    }
                }
            }
            force(
                "org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion",
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion",
                "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion",
                "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion",
                "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion",
                "androidx.credentials:credentials:$credentialsVersion",
                "androidx.credentials:credentials-play-services-auth:$credentialsVersion",
                "com.google.android.libraries.identity.googleid:googleid:$googleIdVersion"
            )
        }
    }

    // ── ktlint (koşulsuz, configuration-cache uyumlu) ──
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // ✅ Plugin apply edildikten sonra extension kesin var
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            android.set(true)
            outputToConsole.set(true)

            val strictKtlint =
                providers.gradleProperty("strictKtlint")
                    .orElse(providers.environmentVariable("STRICT_KTLINT"))
                    .map { it.equals("true", ignoreCase = true) }
                    .orElse(false)
                    .get()

            // Ktlint'i varsayılan olarak advisory tutuyoruz.
            // Böylece düşük değerli format/yerleşim ihlalleri CI'yi kırmaz.
            // İstenirse -PstrictKtlint=true veya STRICT_KTLINT=true ile tekrar bloklayıcı yapılabilir.
            ignoreFailures.set(!strictKtlint)

            reporters {
                reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
                reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
            }
        }

        rootProject.tasks.named(qualityCheckTask.name).configure {
            dependsOn(tasks.named("ktlintCheck"))
        }
    }

    // ── Java Toolchain 21 Enforce ──
    extensions.findByType<JavaPluginExtension>()?.toolchain?.languageVersion?.set(
        JavaLanguageVersion.of(21)
    )
    extensions.findByType<KotlinBaseExtension>()?.jvmToolchain(21)

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")

            // Compose Compiler Metrics
            if (project.findProperty("composeCompilerReports") == "true") {
                freeCompilerArgs.addAll(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" + project.layout.buildDirectory.dir(
                        "compose_reports"
                    ).get().asFile.absolutePath,
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + project.layout.buildDirectory.dir(
                        "compose_metrics"
                    ).get().asFile.absolutePath
                )
            }
        }
    }

    // ── Test Control (enabled by default, optional disable via -PdisableTests=true) ──
    val disableTests = (findProperty("disableTests") as String?)?.toBoolean() == true
    if (disableTests) {
        tasks.withType<Test>().configureEach { enabled = false }
        tasks.matching { it.name.contains("UnitTest") }.configureEach { enabled = false }
    }

    // ── Android Lint for library modules ──
    plugins.withId("com.android.library") {
        @Suppress("UNCHECKED_CAST") val androidExt =
            extensions.getByName("android") as com.android.build.api.dsl.LibraryExtension

        androidExt.packaging {
            jniLibs {
                // Prebuilt libs may not be strip-compatible; keep symbols to avoid noisy warnings.
                keepDebugSymbols += setOf(
                    "**/libandroidx.graphics.path.so", "**/libdatastore_shared_counter.so"
                )
            }
        }

        androidExt.lint {
            abortOnError = true
            checkAllWarnings = true
            warningsAsErrors = false
            checkDependencies = false
            htmlReport = true
            xmlReport = true
            sarifReport = true
            baseline = file("lint-baseline.xml")
            // Ignore gRPC/Firebase library issues with javax.naming (not available on Android)
            disable.add("InvalidPackage")
        }

        // ProGuard Consumer Rules
        val consumerRules = file("consumer-rules.pro")
        if (consumerRules.exists()) {
            androidExt.defaultConfig.consumerProguardFiles(consumerRules)
        }

        tasks.matching { it.name == "lintDebug" || it.name == "lintRelease" }.configureEach {
            rootProject.tasks.named(qualityCheckTask.name).configure { dependsOn(this@configureEach) }
        }
    }

    plugins.withId("com.android.application") {
        tasks.matching { it.name == "lintDebug" || it.name == "lintRelease" }.configureEach {
            rootProject.tasks.named(qualityCheckTask.name).configure { dependsOn(this@configureEach) }
        }
    }
}

// qualityCheck task is registered near the top so subproject hooks can safely depend on it.

// ═══════════════════════════════════════════════════════════════
// ▸ 3b. Kover — Unit test coverage aggregation
//    Run: ./gradlew koverXmlReport   (CI)
//    Run: ./gradlew koverHtmlReport  (local preview in build/reports/kover/html)
// ═══════════════════════════════════════════════════════════════

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

kover {
    merge {
        subprojects()
    }
    reports {
        filters {
            excludes {
                annotatedBy("Generated", "Composable")
                packages(
                    "*.BuildConfig",
                    "hilt_aggregated_deps.*",
                    "*.di",
                    "*.di.*",
                )
                // Exclude generated Room DAO implementations
                classes("*_Impl", "*_Impl\$*")
            }
        }
        total {
            html {
                htmlDir.set(layout.buildDirectory.dir("reports/kover/html"))
            }
            xml {
                xmlFile.set(layout.buildDirectory.file("reports/kover/xml/coverage.xml"))
            }
        }
        verify {
            rule("Minimum line coverage") {
                bound {
                    minValue = 10
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ▸ 4. printFlavors — Utility for CI/CD to get flavor list
//    Usage: ./gradlew -q printFlavors
// ═══════════════════════════════════════════════════════════════
tasks.register("printFlavors") {
    description = "Prints all product flavors as a JSON array for CI matrix generation"
    group = "help"
    doLast {
        // Output format: ["flavor1","flavor2"]
        val flavors = AppFlavors.all.joinToString(
            prefix = "[", separator = ",", postfix = "]"
        ) { "\"${it.name}\"" }
        println(flavors)
    }
}

// ═══════════════════════════════════════════════════════════════
// ▸ 5. composeReports — Generate Compose Compiler stability & metrics reports
//    Usage: ./gradlew composeReports
//    Output: build/compose_reports/ and build/compose_metrics/ in each module
// ═══════════════════════════════════════════════════════════════
tasks.register("composeReports") {
    description = "Generate Compose Compiler stability/metrics reports for all modules"
    group = "verification"
    doLast {
        println("═══════════════════════════════════════════════════════")
        println("  Compose Compiler Reports")
        println("═══════════════════════════════════════════════════════")
        println()
        println("To generate reports, run:")
        println("  ./gradlew assembleRelease -PcomposeCompilerReports=true")
        println()
        println("Reports will be generated in each module's build directory:")
        println("  <module>/build/compose_reports/    (stability reports)")
        println("  <module>/build/compose_metrics/    (metrics reports)")
        println()
        println("Key files to look for:")
        println("  *-composables.txt    — List of all composables with stability info")
        println("  *-composables.csv    — CSV of composable metrics (restartable, skippable)")
        println("  *-classes.txt        — Class stability analysis")
        println("  *-module.json        — Module-level summary metrics")
        println()

        // Find and list existing report directories
        var found = false
        subprojects.forEach { sub ->
            val reportsDir = sub.layout.buildDirectory.dir("compose_reports").get().asFile
            val metricsDir = sub.layout.buildDirectory.dir("compose_metrics").get().asFile
            if (reportsDir.exists() || metricsDir.exists()) {
                found = true
                println("Found reports for :${sub.name}")
                if (reportsDir.exists()) {
                    reportsDir.listFiles()?.forEach { f -> println("  📄 ${f.name}") }
                }
                if (metricsDir.exists()) {
                    metricsDir.listFiles()?.forEach { f -> println("  📊 ${f.name}") }
                }
                println()
            }
        }
        if (!found) {
            println("No compose reports found yet. Run the assembleRelease command above first.")
        }
    }
}
