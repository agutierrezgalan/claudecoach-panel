@file:OptIn(ExperimentalWasmJsInterop::class)

package coach.platform

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.events.Event
import kotlin.js.Promise

// Declaraciones tipadas de las APIs del navegador que usa la app.
// Único sitio con interop JS: el resto del código no sabe que corre en un navegador.

// ---------- WebCrypto ----------
external interface CryptoKey : JsAny

external interface SubtleCrypto : JsAny {
    fun importKey(
        format: String,
        keyData: Uint8Array,
        algorithm: String,
        extractable: Boolean,
        keyUsages: JsArray<JsString>,
    ): Promise<CryptoKey>

    fun deriveKey(
        algorithm: JsAny,
        baseKey: CryptoKey,
        derivedKeyType: JsAny,
        extractable: Boolean,
        keyUsages: JsArray<JsString>,
    ): Promise<CryptoKey>

    fun decrypt(algorithm: JsAny, key: CryptoKey, data: Uint8Array): Promise<ArrayBuffer>
}

external interface Crypto : JsAny {
    val subtle: SubtleCrypto?
}

// Parámetros de algoritmo: objetos JS planos (diccionarios WebIDL).
internal fun pbkdf2Params(salt: Uint8Array, iterations: Int): JsAny =
    js("({ name: 'PBKDF2', hash: 'SHA-256', salt: salt, iterations: iterations })")

internal fun aesGcmKeyType(): JsAny = js("({ name: 'AES-GCM', length: 256 })")

internal fun aesGcmParams(iv: Uint8Array, aad: Uint8Array): JsAny =
    js("({ name: 'AES-GCM', iv: iv, additionalData: aad })")

internal fun usages(usage: String): JsArray<JsString> = js("[usage]")

internal fun globalCrypto(): Crypto? = js("globalThis.crypto ?? null")

// ---------- Codificación ----------
internal fun base64ToBytes(b64: String): Uint8Array = js("Uint8Array.from(atob(b64), (c) => c.charCodeAt(0))")

internal fun utf8Encode(text: String): Uint8Array = js("new TextEncoder().encode(text)")

internal fun utf8Decode(buffer: ArrayBuffer): String = js("new TextDecoder().decode(buffer)")

// ---------- IndexedDB ----------
external interface IDBFactory : JsAny {
    fun open(name: String, version: Int): IDBOpenDBRequest
}

external interface IDBRequest : JsAny {
    val result: JsAny?
    var onsuccess: ((Event) -> Unit)?
    var onerror: ((Event) -> Unit)?
}

external interface IDBOpenDBRequest : IDBRequest {
    var onupgradeneeded: ((Event) -> Unit)?
}

external interface IDBDatabase : JsAny {
    fun createObjectStore(name: String): JsAny
    fun transaction(storeName: String, mode: String): IDBTransaction
    fun close()
}

external interface IDBTransaction : JsAny {
    fun objectStore(name: String): IDBObjectStore
    var oncomplete: ((Event) -> Unit)?
    var onerror: ((Event) -> Unit)?
    var onabort: ((Event) -> Unit)?
}

external interface IDBObjectStore : JsAny {
    fun get(key: String): IDBRequest
    fun put(value: JsAny, key: String): IDBRequest
    fun delete(key: String): IDBRequest
}

internal fun globalIndexedDb(): IDBFactory? = js("globalThis.indexedDB ?? null")

/** Única conversión sin comprobar: IndexedDB devuelve `result` sin tipo. */
internal fun <T : JsAny> JsAny.assumeType(): T = unsafeCast()

// ---------- fetch ----------
// Ojo: el constructor RequestInit(...) de kotlinx-browser envía `headers: null`, que el navegador rechaza.
// Por eso se construye solo con los campos necesarios.
internal fun noStoreInit(): org.w3c.fetch.RequestInit =
    js("({ cache: 'no-store', credentials: 'omit', referrerPolicy: 'no-referrer' })")

/** `fetch` global (ventana en el navegador, undici en Node para los tests). */
internal fun platformFetch(url: String, init: org.w3c.fetch.RequestInit): Promise<org.w3c.fetch.Response> =
    js("globalThis.fetch(url, init)")

// ---------- Fecha y formato del navegador ----------
internal fun nowMillis(): Double = js("Date.now()")

internal fun locationHostname(): String = js("globalThis.location?.hostname ?? ''")

internal fun locationPathname(): String = js("globalThis.location?.pathname ?? '/'")

internal fun dateLabelEs(iso: String, long: Boolean): String = js(
    "new Date(iso.slice(0, 10) + 'T12:00:00').toLocaleDateString('es-ES', long ? " +
        "{ weekday: 'short', day: 'numeric', month: 'short' } : { day: 'numeric', month: 'short' })",
)
