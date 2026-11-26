@file:OptIn(ExperimentalWasmJsInterop::class)

package coach.platform

import coach.core.Outcome
import coach.data.DataLocation
import coach.data.EncryptedSource
import coach.data.LoadError
import kotlinx.coroutines.await
import org.w3c.fetch.Response

/** Descarga el sobre cifrado sin caché, sin cookies y sin referer. */
class FetchSource(private val url: () -> String = ::defaultUrl) : EncryptedSource {
    override suspend fun fetch(): Outcome<String, LoadError> {
        val response = try {
            platformFetch(url(), noStoreInit()).await<Response>()
        } catch (e: Throwable) {
            return Outcome.Err(LoadError.Network(status = null))
        }
        val status = response.status.toInt()
        return when {
            status == 404 -> Outcome.Err(LoadError.NoData)
            !response.ok -> Outcome.Err(LoadError.Network(status = status))
            else -> try {
                Outcome.Ok(response.text().await<JsString>().toString())
            } catch (e: Throwable) {
                Outcome.Err(LoadError.Network(status = status))
            }
        }
    }

    private companion object {
        fun defaultUrl(): String = DataLocation.url(locationHostname(), locationPathname(), nowMillis().toLong())
    }
}
