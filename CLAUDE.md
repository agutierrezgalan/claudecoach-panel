# claudecoach-panel

Repo PÚBLICO. Nunca subas datos sin cifrar, secretos ni el `data.json` a `main`.

- App: Kotlin/Wasm (`src/wasmJsMain/kotlin/coach/`), recursos web en `src/wasmJsMain/resources/`.
- Formato de datos: lo genera `entreno/panel.py` en el repo privado ClaudeCoach (modelos en `Model.kt`).
- Seguridad: no relajes la CSP; los datos se pintan con `textContent`/atributos, nunca como HTML;
  la descifrado usa WebCrypto (Interop.kt).
- Dependencias: si cambias versiones, regenera `gradle/verification-metadata.xml`
  (`gradle --write-verification-metadata sha256 wasmJsBrowserDistribution`) y el lock de npm
  (`gradle kotlinWasmUpgradePackageLock`). Versiones con al menos 2 semanas de antigüedad.
