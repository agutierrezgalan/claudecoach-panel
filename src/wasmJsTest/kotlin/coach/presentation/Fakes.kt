package coach.presentation

import coach.core.Outcome
import coach.data.Cipher
import coach.data.EncryptedSource
import coach.data.Envelope
import coach.data.KeyStore
import coach.data.LoadError
import coach.data.SecretKey
import coach.fixtures.ContractFixtures

class FakeSource(var result: Outcome<String, LoadError> = Outcome.Ok(ContractFixtures.ENCRYPTED)) : EncryptedSource {
    var calls = 0
    override suspend fun fetch(): Outcome<String, LoadError> { calls++; return result }
}

class FakeKey(val password: String) : SecretKey

/** Cifrado de mentira: la "clave" recuerda la contraseña y solo "buena" descifra. */
class FakeCipher(var plaintext: String = ContractFixtures.PLAIN) : Cipher {
    override suspend fun deriveKey(password: String, envelope: Envelope): Outcome<SecretKey, LoadError> =
        Outcome.Ok(FakeKey(password))
    override suspend fun decrypt(key: SecretKey, envelope: Envelope): Outcome<String, LoadError> =
        if ((key as? FakeKey)?.password == "buena") Outcome.Ok(plaintext) else Outcome.Err(LoadError.WrongPassword)
    fun keyFor(password: String): SecretKey = FakeKey(password)
}

class FakeKeyStore(var stored: SecretKey? = null) : KeyStore {
    var saves = 0
    var clears = 0
    override suspend fun load(): SecretKey? = stored
    override suspend fun save(key: SecretKey) { saves++; stored = key }
    override suspend fun clear() { clears++; stored = null }
}
