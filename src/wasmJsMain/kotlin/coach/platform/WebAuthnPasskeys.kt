@file:OptIn(ExperimentalWasmJsInterop::class)

package coach.platform

import coach.core.Outcome
import coach.data.ApiErrors
import coach.data.LoadError
import coach.data.Passkeys
import kotlinx.coroutines.await
import kotlin.js.Promise

/** Passkeys del navegador (Face ID / Touch ID) con navigator.credentials. */
class WebAuthnPasskeys private constructor() : Passkeys {
    override suspend fun create(optionsJson: String): Outcome<String, LoadError> = run(passkeyCreate(optionsJson))

    override suspend fun get(optionsJson: String): Outcome<String, LoadError> = run(passkeyGet(optionsJson))

    private suspend fun run(promise: Promise<JsString>): Outcome<String, LoadError> {
        val result = try {
            promise.await<JsString>().toString()
        } catch (e: Throwable) {
            return Outcome.Err(LoadError.PasskeyRejected)
        }
        return if (result.startsWith("ERR:")) Outcome.Err(ApiErrors.ofPasskey(result.removePrefix("ERR:")))
        else Outcome.Ok(result)
    }

    companion object {
        fun create(): Outcome<Passkeys, LoadError> =
            if (passkeysSupported()) Outcome.Ok(WebAuthnPasskeys()) else Outcome.Err(LoadError.NoPasskeySupport)
    }
}
