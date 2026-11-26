import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("coach")
        browser {
            commonWebpackConfig {
                outputFileName = "coach.js"
            }
        }
        binaries.executable()
    }
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
            }
        }
    }
}

// Seguridad de la cadena de suministro: el lock de npm es obligatorio y no se reescribe solo.
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmResolverPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension>().apply {
        packageLockMismatchReport.set(org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport.FAIL)
        packageLockAutoReplace.set(false)
        reportNewPackageLock.set(true)
    }
}
