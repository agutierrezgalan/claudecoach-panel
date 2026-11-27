package coach.data

import coach.core.Outcome

/** Token de sesión opaco que entrega el backend tras Face ID (la app no lo interpreta). */
value class SessionToken(val value: String)

/** API del backend (Cloudflare). Las opciones y respuestas de WebAuthn viajan como JSON sin tocar. */
interface PanelApi {
    suspend fun registrationOptions(password: String): Outcome<String, LoadError>
    suspend fun register(credentialJson: String): Outcome<Unit, LoadError>
    suspend fun loginOptions(): Outcome<String, LoadError>
    suspend fun login(assertionJson: String): Outcome<SessionToken, LoadError>
    suspend fun panel(token: SessionToken): Outcome<String, LoadError>
    suspend fun logout(token: SessionToken)
}

/** Passkeys del dispositivo (Face ID / Touch ID): crear una y firmar un reto con ella. */
interface Passkeys {
    suspend fun create(optionsJson: String): Outcome<String, LoadError>
    suspend fun get(optionsJson: String): Outcome<String, LoadError>
}

/** Dónde se guarda el token de sesión en este dispositivo. */
interface SessionStore {
    suspend fun load(): SessionToken?
    suspend fun save(token: SessionToken)
    suspend fun clear()
}
