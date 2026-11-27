@file:OptIn(ExperimentalWasmJsInterop::class)

package coach.platform

import org.w3c.dom.events.Event
import kotlin.js.Promise

// Declaraciones tipadas de las APIs del navegador que usa la app.
// Único sitio con interop JS: el resto del código no sabe que corre en un navegador.

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

external interface DOMStringList : JsAny {
    fun contains(name: String): Boolean
}

external interface IDBDatabase : JsAny {
    val objectStoreNames: DOMStringList
    fun createObjectStore(name: String): JsAny
    fun deleteObjectStore(name: String)
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

// ---------- API del backend ----------

/** Respuesta reducida a lo que usa la app: código HTTP (0 = sin conexión) y cuerpo. */
external interface ApiResponse : JsAny {
    val status: Int
    val text: String
}

/** fetch sin caché, sin cookies y sin referer; el token va en la cabecera Authorization. */
internal fun apiFetch(url: String, method: String, body: String?, token: String?): Promise<ApiResponse> = js(
    """globalThis.fetch(url, {
        method, cache: 'no-store', credentials: 'omit', referrerPolicy: 'no-referrer',
        headers: Object.assign({}, body != null ? { 'content-type': 'application/json' } : {},
                               token != null ? { authorization: 'Bearer ' + token } : {}),
        body: body ?? undefined,
    }).then((r) => r.text().then((t) => ({ status: r.status, text: t })), () => ({ status: 0, text: '' }))""",
)

// ---------- Passkeys (WebAuthn) ----------
// Opciones y respuestas viajan como JSON (base64url para los binarios), como las genera y espera el backend.
// Un error del navegador vuelve como 'ERR:<nombre del DOMException>'.

internal fun passkeysSupported(): Boolean =
    js("typeof globalThis.PublicKeyCredential !== 'undefined' && !!globalThis.navigator?.credentials")

internal fun passkeyCreate(optionsJson: String): Promise<JsString> = js(
    """(() => {
        const dec = (s) => Uint8Array.from(atob(s.replace(/-/g, '+').replace(/_/g, '/')), (c) => c.charCodeAt(0)).buffer;
        const enc = (b) => btoa(String.fromCharCode(...new Uint8Array(b))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
        const o = JSON.parse(optionsJson);
        o.challenge = dec(o.challenge);
        o.user.id = dec(o.user.id);
        (o.excludeCredentials || []).forEach((c) => { c.id = dec(c.id); });
        return navigator.credentials.create({ publicKey: o }).then((c) => JSON.stringify({
            id: c.id, rawId: enc(c.rawId), type: c.type, authenticatorAttachment: c.authenticatorAttachment,
            clientExtensionResults: c.getClientExtensionResults(),
            response: { clientDataJSON: enc(c.response.clientDataJSON), attestationObject: enc(c.response.attestationObject),
                        transports: c.response.getTransports ? c.response.getTransports() : [] },
        }), (e) => 'ERR:' + ((e && e.name) || 'Error'));
    })()""",
)

internal fun passkeyGet(optionsJson: String): Promise<JsString> = js(
    """(() => {
        const dec = (s) => Uint8Array.from(atob(s.replace(/-/g, '+').replace(/_/g, '/')), (c) => c.charCodeAt(0)).buffer;
        const enc = (b) => btoa(String.fromCharCode(...new Uint8Array(b))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
        const o = JSON.parse(optionsJson);
        o.challenge = dec(o.challenge);
        (o.allowCredentials || []).forEach((c) => { c.id = dec(c.id); });
        return navigator.credentials.get({ publicKey: o }).then((c) => JSON.stringify({
            id: c.id, rawId: enc(c.rawId), type: c.type, authenticatorAttachment: c.authenticatorAttachment,
            clientExtensionResults: c.getClientExtensionResults(),
            response: { clientDataJSON: enc(c.response.clientDataJSON), authenticatorData: enc(c.response.authenticatorData),
                        signature: enc(c.response.signature),
                        userHandle: c.response.userHandle ? enc(c.response.userHandle) : undefined },
        }), (e) => 'ERR:' + ((e && e.name) || 'Error'));
    })()""",
)

// ---------- Fecha y formato del navegador ----------
internal fun nowMillis(): Double = js("Date.now()")

internal fun locationHostname(): String = js("globalThis.location?.hostname ?? ''")

internal fun locationPathname(): String = js("globalThis.location?.pathname ?? '/'")

internal fun dateLabelEs(iso: String, long: Boolean): String = js(
    "new Date(iso.slice(0, 10) + 'T12:00:00').toLocaleDateString('es-ES', long ? " +
        "{ weekday: 'short', day: 'numeric', month: 'short' } : { day: 'numeric', month: 'short' })",
)
