package coach.data

import coach.core.Outcome
import coach.core.flatMap

/** Reglas de negocio de la carga: descarga, clave recordada, desbloqueo y olvido. */
class PanelRepository(
    private val source: EncryptedSource,
    private val cipher: Cipher,
    private val keys: KeyStore,
) {
    suspend fun envelope(): Outcome<Envelope, LoadError> = source.fetch().flatMap(PayloadCodec::decodeEnvelope)

    /**
     * Abre con la clave recordada. Devuelve null si no hay clave o si ya no vale
     * (contraseña cambiada): en ese caso la borra para no reintentar con ella.
     */
    suspend fun openWithStoredKey(envelope: Envelope): Outcome<Payload, LoadError>? {
        val key = keys.load() ?: return null
        val result = open(key, envelope)
        if (result is Outcome.Err && result.error == LoadError.WrongPassword) {
            keys.clear()
            return null
        }
        return result
    }

    /** Deriva la clave de la contraseña; solo se recuerda si abre los datos y se pidió. */
    suspend fun unlock(envelope: Envelope, password: String, remember: Boolean): Outcome<Payload, LoadError> =
        cipher.deriveKey(password, envelope).flatMap { key ->
            val result = open(key, envelope)
            if (result is Outcome.Ok && remember) keys.save(key)
            result
        }

    suspend fun forget() = keys.clear()

    private suspend fun open(key: SecretKey, envelope: Envelope): Outcome<Payload, LoadError> =
        cipher.decrypt(key, envelope).flatMap(PayloadCodec::decodePayload)
}
