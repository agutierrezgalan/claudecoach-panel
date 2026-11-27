@file:OptIn(ExperimentalWasmJsInterop::class)

package coach.platform

import coach.core.Outcome
import coach.data.ApiCall
import coach.data.ApiErrors
import coach.data.DataLocation
import coach.data.LoadError
import coach.data.PanelApi
import coach.data.SessionToken
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Cliente de la API del backend (Cloudflare): passkeys, sesión y datos del panel. */
class HttpPanelApi(private val url: (String) -> String = DataLocation::url) : PanelApi {
    override suspend fun registrationOptions(password: String): Outcome<String, LoadError> =
        call(ApiCall.REGISTER, "POST", "/auth/register/options", body(mapOf("password" to JsonPrimitive(password))))

    override suspend fun register(credentialJson: String): Outcome<Unit, LoadError> =
        when (val r = call(ApiCall.REGISTER_VERIFY, "POST", "/auth/register/verify", wrap(credentialJson))) {
            is Outcome.Ok -> Outcome.Ok(Unit)
            is Outcome.Err -> r
        }

    override suspend fun loginOptions(): Outcome<String, LoadError> =
        call(ApiCall.LOGIN_OPTIONS, "POST", "/auth/login/options", null)

    override suspend fun login(assertionJson: String): Outcome<SessionToken, LoadError> =
        when (val r = call(ApiCall.LOGIN, "POST", "/auth/login/verify", wrap(assertionJson))) {
            is Outcome.Err -> r
            is Outcome.Ok -> try {
                val token = Json.parseToJsonElement(r.value).jsonObject["token"]?.jsonPrimitive?.contentOrNull
                if (token.isNullOrBlank()) Outcome.Err(LoadError.Corrupt("sin token")) else Outcome.Ok(SessionToken(token))
            } catch (e: IllegalArgumentException) {
                Outcome.Err(LoadError.Corrupt("respuesta de login"))
            }
        }

    override suspend fun panel(token: SessionToken): Outcome<String, LoadError> =
        call(ApiCall.PANEL, "GET", "/v1/panel", null, token)

    override suspend fun logout(token: SessionToken) {
        call(ApiCall.PANEL, "POST", "/auth/logout", null, token)
    }

    private suspend fun call(kind: ApiCall, method: String, path: String, body: String?, token: SessionToken? = null):
        Outcome<String, LoadError> {
        val res = try {
            apiFetch(url(path), method, body, token?.value).await<ApiResponse>()
        } catch (e: Throwable) {
            return Outcome.Err(LoadError.Network(null))
        }
        val status = res.status
        return when {
            status == 0 -> Outcome.Err(LoadError.Network(null))
            status in 200..299 -> Outcome.Ok(res.text)
            else -> Outcome.Err(ApiErrors.of(status, kind))
        }
    }

    private fun body(fields: Map<String, JsonPrimitive>): String = JsonObject(fields).toString()

    /** {"response": <respuesta de WebAuthn tal cual>} */
    private fun wrap(json: String): String = buildJsonObject { put("response", Json.parseToJsonElement(json)) }.toString()
}
