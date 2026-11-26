@file:OptIn(ExperimentalWasmJsInterop::class)

package coach

import kotlin.js.Promise

// Puente mínimo con APIs del navegador (WebCrypto, IndexedDB, Intl, fetch).
// Todo el código JS va compilado en el módulo: nada de eval, compatible con la CSP.

/** Deriva una clave AES-GCM-256 NO extraíble con PBKDF2-SHA256. */
fun deriveKey(password: String, saltB64: String, iterations: Int): Promise<JsAny> = js("""
  (async () => {
    const salt = Uint8Array.from(atob(saltB64), c => c.charCodeAt(0));
    const base = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveKey']);
    return crypto.subtle.deriveKey({ name: 'PBKDF2', hash: 'SHA-256', salt, iterations },
      base, { name: 'AES-GCM', length: 256 }, false, ['decrypt']);
  })()
""")

/** Descifra y devuelve el texto (JSON). Rechaza si la clave no es correcta. */
fun decryptText(key: JsAny, ivB64: String, ctB64: String, aad: String): Promise<JsString> = js("""
  (async () => {
    const b = s => Uint8Array.from(atob(s), c => c.charCodeAt(0));
    const pt = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: b(ivB64), additionalData: new TextEncoder().encode(aad) }, key, b(ctB64));
    return new TextDecoder().decode(pt);
  })()
""")

/** Guarda / lee / borra la CryptoKey (no extraíble) en IndexedDB. Fallos = sin recordar. */
fun keyLoad(): Promise<JsAny?> = js("""
  new Promise(res => {
    try {
      const r = indexedDB.open('claudecoach', 1);
      r.onupgradeneeded = () => r.result.createObjectStore('k');
      r.onerror = () => res(null);
      r.onsuccess = () => {
        try {
          const q = r.result.transaction('k').objectStore('k').get('aes');
          q.onsuccess = () => res(q.result || null);
          q.onerror = () => res(null);
        } catch (e) { res(null); }
      };
    } catch (e) { res(null); }
  })
""")

fun keySave(key: JsAny): Promise<JsAny?> = js("""
  new Promise(res => {
    try {
      const r = indexedDB.open('claudecoach', 1);
      r.onupgradeneeded = () => r.result.createObjectStore('k');
      r.onerror = () => res(null);
      r.onsuccess = () => {
        try {
          const tx = r.result.transaction('k', 'readwrite');
          tx.objectStore('k').put(key, 'aes');
          tx.oncomplete = () => res(null);
          tx.onerror = () => res(null);
        } catch (e) { res(null); }
      };
    } catch (e) { res(null); }
  })
""")

fun keyForget(): Promise<JsAny?> = js("""
  new Promise(res => {
    try {
      const r = indexedDB.open('claudecoach', 1);
      r.onupgradeneeded = () => r.result.createObjectStore('k');
      r.onerror = () => res(null);
      r.onsuccess = () => {
        try {
          const tx = r.result.transaction('k', 'readwrite');
          tx.objectStore('k').delete('aes');
          tx.oncomplete = () => res(null);
          tx.onerror = () => res(null);
        } catch (e) { res(null); }
      };
    } catch (e) { res(null); }
  })
""")

/** Descarga texto sin caché. */
fun fetchText(url: String): Promise<JsString> = js("""
  fetch(url, { cache: 'no-store', credentials: 'omit', referrerPolicy: 'no-referrer' }).then(r => {
    if (!r.ok) throw new Error('HTTP ' + r.status);
    return r.text();
  })
""")

/**
 * En GitHub Pages los datos cifrados se leen de la rama `data` del mismo repo público
 * (así no hace falta recompilar la app cada día). En local, de ./data.json.
 */
fun dataUrl(): String = js("""
  (() => {
    const h = location.hostname, seg = location.pathname.split('/').filter(Boolean);
    if (h.endsWith('.github.io') && seg.length) {
      const owner = h.split('.')[0];
      return 'https://raw.githubusercontent.com/' + owner + '/' + seg[0] + '/data/data.json?t=' + Date.now();
    }
    return 'data.json?t=' + Date.now();
  })()
""")

fun hasWebCrypto(): Boolean = js("!!(window.crypto && crypto.subtle)")

fun fmtNum(v: Double, maxFrac: Int): String =
    js("new Intl.NumberFormat('es-ES', { maximumFractionDigits: maxFrac }).format(v)")

fun dayLabel(iso: String): String =
    js("new Date(iso.slice(0, 10) + 'T12:00:00').toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric', month: 'short' })")

fun shortDayLabel(iso: String): String =
    js("new Date(iso.slice(0, 10) + 'T12:00:00').toLocaleDateString('es-ES', { day: 'numeric', month: 'short' })")

fun reloadPage(): Unit = js("location.reload()")

fun viewportWidth(): Double = js("window.innerWidth")
