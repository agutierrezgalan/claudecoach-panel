# ClaudeCoach · panel

Panel web de entrenamiento escrito en **Kotlin/Wasm** (DOM, sin frameworks) y publicado con GitHub Pages:
https://agutierrezgalan.github.io/claudecoach-panel/

- Los datos los sirve el **backend privado** de ClaudeCoach (Cloudflare Workers) y solo tras entrar con una
  **passkey** (Face ID / Touch ID). La primera vez en cada dispositivo se activa con la contraseña del panel.
- Sesión de 30 días: el token se guarda en IndexedDB (el backend solo guarda su hash).
- CSP estricta: solo scripts propios + Wasm; conexiones solo al propio sitio y al backend.
- Requiere un navegador con WebAssembly GC (iOS/Safari 18.2+, Chrome 119+).

## Compilar y probar
```bash
gradle wasmJsNodeTest              # tests (contrato, flujos de passkey y sesión, presentadores)
gradle wasmJsBrowserDistribution   # salida: build/dist/wasmJs/productionExecutable
```
Arquitectura por capas (core, data, platform, presentation, ui): ver `CLAUDE.md`.
Dependencias fijadas: versiones exactas, `gradle/verification-metadata.xml` (SHA-256 de cada artefacto,
Node y binaryen incluidos) y `kotlin-js-store/wasm/package-lock.json` (el build falla si cambia).
Para probar en local: `wrangler dev` del backend con `PANEL_ORIGIN`/`RP_ID` de localhost y un autenticador virtual.
