import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.util.Base64

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// Fixtures del contrato (copiados de ClaudeCoach/contract) -> objeto Kotlin para los tests.
val contractFixtures = tasks.register("generateContractFixtures") {
    val inDir = layout.projectDirectory.dir("contract/fixtures")
    val outDir = layout.buildDirectory.dir("generated/contract/kotlin")
    inputs.dir(inDir)
    outputs.dir(outDir)
    doLast {
        fun b64(name: String) = Base64.getEncoder().encodeToString(inDir.file(name).asFile.readBytes())
        val file = outDir.get().file("coach/fixtures/ContractFixtures.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |// Generado por la tarea generateContractFixtures. No editar.
            |package coach.fixtures
            |
            |import kotlin.io.encoding.Base64
            |
            |object ContractFixtures {
            |    /** Contraseña pública del fixture SINTÉTICO (ver ClaudeCoach/entreno/contract.py). */
            |    const val PASSWORD: String = "contrato-de-prueba-123"
            |    val PLAIN: String = Base64.decode("${b64("panel-v2.json")}").decodeToString()
            |    val ENCRYPTED: String = Base64.decode("${b64("panel-v2.enc.json")}").decodeToString()
            |}
            |""".trimMargin(),
        )
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("coach")
        browser {
            commonWebpackConfig {
                outputFileName = "coach.js"
            }
            // La lógica se prueba en Node (wasmJsNodeTest); el navegador se prueba de punta a punta aparte.
            testTask { enabled = false }
        }
        // Tests en Node.js (WebCrypto disponible en globalThis.crypto); el DOM solo en el navegador.
        nodejs()
        binaries.executable()
    }
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
    sourceSets {
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        wasmJsTest.configure { kotlin.srcDir(contractFixtures) }
        wasmJsTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
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
