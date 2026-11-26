# ClaudeCoach · panel

Panel web de entrenamiento escrito en **Kotlin/Wasm** (DOM, sin frameworks) y publicado con GitHub Pages:
https://agutierrezgalan.github.io/claudecoach-panel/

- Los datos llegan **cifrados** (AES-256-GCM, clave PBKDF2-SHA256 de 600 000 iteraciones) a la rama `data`
  desde el repo privado ClaudeCoach. Se descifran solo en tu dispositivo con tu contraseña.
- La clave derivada puede quedarse en IndexedDB como clave **no extraíble** («Recordar en este dispositivo»).
- CSP estricta: solo scripts propios + Wasm; datos leídos únicamente de `raw.githubusercontent.com`.
- Requiere un navegador con WebAssembly GC (iOS/Safari 18.2+, Chrome 119+).

## Compilar y probar
```bash
gradle wasmJsNodeTest              # tests (contrato con Python, cifrado real, estados, presentadores)
gradle wasmJsBrowserDistribution   # salida: build/dist/wasmJs/productionExecutable
```
Arquitectura por capas (core, data, platform, presentation, ui): ver `CLAUDE.md`.
Dependencias fijadas: versiones exactas, `gradle/verification-metadata.xml` (SHA-256 de cada artefacto,
Node y binaryen incluidos) y `kotlin-js-store/wasm/package-lock.json` (el build falla si cambia).
Para probar en local, copia un `data.json` cifrado junto al `index.html` generado.
