package coach.data

import coach.core.Outcome
import coach.core.flatMap

/** Reglas de la carga: sesión guardada, Face ID, registro de la passkey y cierre de sesión. */
class PanelRepository(
    private val api: PanelApi,
    private val passkeys: Passkeys,
    private val sessions: SessionStore,
) {
    /** Abre con la sesión guardada; null si no hay o ha caducado (entonces se olvida). */
    suspend fun openWithSession(): Outcome<Payload, LoadError>? {
        val token = sessions.load() ?: return null
        val result = load(token)
        if (result is Outcome.Err && result.error == LoadError.Unauthorized) {
            sessions.clear()
            return null
        }
        return result
    }

    /** Face ID -> sesión nueva -> datos. */
    suspend fun loginWithPasskey(): Outcome<Payload, LoadError> =
        api.loginOptions().flatMap { passkeys.get(it) }.flatMap { api.login(it) }.flatMap { token ->
            sessions.save(token)
            load(token)
        }

    /** Primera vez en un dispositivo: la contraseña del panel autoriza crear la passkey; luego entra. */
    suspend fun registerPasskey(password: String): Outcome<Payload, LoadError> =
        api.registrationOptions(password).flatMap { passkeys.create(it) }.flatMap { api.register(it) }
            .flatMap { loginWithPasskey() }

    suspend fun logout() {
        sessions.load()?.let { api.logout(it) }
        sessions.clear()
    }

    private suspend fun load(token: SessionToken): Outcome<Payload, LoadError> =
        api.panel(token).flatMap { PayloadCodec.decodePayload(it) }
}
