@file:OptIn(ExperimentalWasmJsInterop::class)

package coach.platform

import coach.data.SessionStore
import coach.data.SessionToken
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Guarda el token de sesión en IndexedDB (no en localStorage). Si IndexedDB falla (modo privado,
 * bloqueado), se comporta como «sin sesión»: se vuelve a pedir Face ID, nunca rompe la app.
 */
class IndexedDbSessionStore(
    private val dbName: String = "claudecoach",
    private val store: String = "s",
    private val entry: String = "session",
) : SessionStore {

    override suspend fun load(): SessionToken? =
        withStore("readonly") { it.get(entry) }?.let { SessionToken(it.assumeType<JsString>().toString()) }

    override suspend fun save(token: SessionToken) {
        withStore("readwrite") { it.put(token.value.toJsString(), entry) }
    }

    override suspend fun clear() {
        withStore("readwrite") { it.delete(entry) }
    }

    private suspend fun openDb(): IDBDatabase? {
        val factory = globalIndexedDb() ?: return null
        return suspendCancellableCoroutine { cont ->
            val req = try {
                factory.open(dbName, 2)
            } catch (e: Throwable) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            req.onupgradeneeded = {
                val db = req.result?.assumeType<IDBDatabase>()
                if (db != null && !db.objectStoreNames.contains(store)) db.createObjectStore(store)
                if (db != null && db.objectStoreNames.contains("k")) db.deleteObjectStore("k") // clave AES antigua
            }
            req.onsuccess = { cont.resume(req.result?.assumeType<IDBDatabase>()) }
            req.onerror = { cont.resume(null) }
        }
    }

    private suspend fun withStore(mode: String, op: (IDBObjectStore) -> IDBRequest): JsAny? {
        val db = openDb() ?: return null
        return try {
            suspendCancellableCoroutine { cont ->
                val request = try {
                    op(db.transaction(store, mode).objectStore(store))
                } catch (e: Throwable) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                request.onsuccess = { cont.resume(request.result) }
                request.onerror = { cont.resume(null) }
            }
        } finally {
            db.close()
        }
    }
}
