# claudecoach-panel

Repo PÚBLICO. Nunca subas datos sin cifrar, secretos ni el `data.json` real a `main`.
Los fixtures de `contract/fixtures/` son SINTÉTICOS (generados en ClaudeCoach) y su contraseña es pública a propósito.

## Arquitectura (`src/wasmJsMain/kotlin/coach/`)
- `core/` — `Outcome<T, E>` (Ok/Err): los errores no cruzan capas como excepciones.
- `data/` — contrato (`Model.kt`, `PayloadCodec` con `SUPPORTED_SCHEMA`), `LoadError` sellado,
  puertos (`EncryptedSource`, `Cipher`, `KeyStore`, `SecretKey` opaco) y `PanelRepository`. Sin tipos JS.
- `platform/` — ÚNICO sitio con interop JS: `WebApis.kt` (externals tipados de WebCrypto, IndexedDB, fetch),
  `WebCryptoCipher`, `IndexedDbKeyStore`, `FetchSource`. El opt-in `ExperimentalWasmJsInterop` solo aquí.
- `presentation/` — `PanelState` sellado, `PanelController` (StateFlow, flujo unidireccional),
  `Presenters` (funciones puras -> modelos de vista), `Messages` (texto de cada error), formato numérico.
- `ui/` — DOM: `PanelView` enlaza estado y gestos; `Sections`/`Charts` pintan modelos de vista.
- `Main.kt` — raíz de composición (único sitio que elige implementaciones).

## Reglas
- TDD: test primero (`src/wasmJsTest/`), luego código. Tests en Node: `gradle wasmJsNodeTest`.
- Avisos = errores (`allWarningsAsErrors`). Nada de `!!`; conversiones sin comprobar solo en `platform`.
- Seguridad: no relajes la CSP; datos solo con `textContent`/atributos; claves AES no extraíbles.
- Contrato: si ClaudeCoach cambia el formato, copia su carpeta `contract/` aquí, ajusta `Model.kt`
  y `PayloadCodec.SUPPORTED_SCHEMA`; los tests de contrato y de cifrado deben pasar.
- Dependencias en `gradle/libs.versions.toml` (≥ 2 semanas de antigüedad). Al cambiarlas:
  `gradle --write-verification-metadata sha256 kotlinWasmUpgradePackageLock wasmJsNodeTest wasmJsBrowserDistribution`.
