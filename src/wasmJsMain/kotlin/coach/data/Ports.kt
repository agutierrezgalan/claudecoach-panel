package coach.data

import coach.core.Outcome

/** Clave opaca: cada plataforma la implementa (en web, una CryptoKey no extraíble). */
interface SecretKey

/** De dónde llega el sobre cifrado (texto JSON). */
interface EncryptedSource {
    suspend fun fetch(): Outcome<String, LoadError>
}

/** Derivación de clave y descifrado del sobre. */
interface Cipher {
    suspend fun deriveKey(password: String, envelope: Envelope): Outcome<SecretKey, LoadError>
    suspend fun decrypt(key: SecretKey, envelope: Envelope): Outcome<String, LoadError>
}

/** Dónde se recuerda la clave en este dispositivo («Recordar en este dispositivo»). */
interface KeyStore {
    suspend fun load(): SecretKey?
    suspend fun save(key: SecretKey)
    suspend fun clear()
}
