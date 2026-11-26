"use strict";
// Muestra un aviso claro si el navegador no puede ejecutar Kotlin/Wasm (WasmGC: iOS 18.2+).
(() => {
  const fail = (msg) => {
    const b = document.getElementById("boot");
    if (b) { b.hidden = false; b.textContent = msg; }
  };
  const isWasm = (m) => /WebAssembly|wasm|CompileError|LinkError/i.test(String(m || ""));
  const MSG = "Tu navegador no puede ejecutar este panel. Actualiza iOS (18.2 o superior) o usa un navegador reciente.";
  window.addEventListener("unhandledrejection", (e) => { if (isWasm(e.reason && (e.reason.message || e.reason))) fail(MSG); });
  window.addEventListener("error", (e) => { if (isWasm(e.message)) fail(MSG); });
  if (typeof WebAssembly !== "object") fail(MSG);
})();
