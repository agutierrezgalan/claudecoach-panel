@file:OptIn(ExperimentalWasmJsInterop::class)

package coach.platform

import coach.data.KeyStore
import coach.data.SecretKey
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Guarda la CryptoKey (no extraíble) en IndexedDB. Si IndexedDB falla (modo privado, bloqueado),
 * se comporta como "no recordar": nunca rompe la app.
 */
class IndexedDbKeyStore(
    private val dbName: String = "claudecoach",
    private val store: String = "k",
    private val entry: String = "aes",
) : KeyStore {

    override suspend fun load(): SecretKey? =
        withStore("readonly") { it.get(entry) }?.let { WebCryptoKey(it.assumeType()) }

    override suspend fun save(key: SecretKey) {
        val cryptoKey = (key as? WebCryptoKey)?.key ?: return
        withStore("readwrite") { it.put(cryptoKey, entry) }
    }

    override suspend fun clear() {
        withStore("readwrite") { it.delete(entry) }
    }

    private suspend fun openDb(): IDBDatabase? {
        val factory = globalIndexedDb() ?: return null
        return suspendCancellableCoroutine { cont ->
            val req = try {
                factory.open(dbName, 1)
            } catch (e: Throwable) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            req.onupgradeneeded = { req.result?.assumeType<IDBDatabase>()?.createObjectStore(store) }
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
