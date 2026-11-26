package coach.platform

import coach.core.Outcome
import coach.data.LoadError
import coach.data.PayloadCodec
import coach.fixtures.ContractFixtures
import coach.ok
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/** Punta a punta: Kotlin descifra exactamente lo que cifró Python (contract/fixtures). */
class WebCryptoCipherTest {
    private val envelope = PayloadCodec.decodeEnvelope(ContractFixtures.ENCRYPTED).ok()

    @Test
    fun decryptsWhatPythonEncrypted() = runTest(timeout = 60.seconds) {
        val cipher = WebCryptoCipher.create().ok()
        val key = cipher.deriveKey(ContractFixtures.PASSWORD, envelope).ok()
        val text = cipher.decrypt(key, envelope).ok()
        assertEquals(PayloadCodec.decodePayload(ContractFixtures.PLAIN).ok(), PayloadCodec.decodePayload(text).ok())
    }

    @Test
    fun wrongPasswordIsATypedError() = runTest(timeout = 60.seconds) {
        val cipher = WebCryptoCipher.create().ok()
        val key = cipher.deriveKey("otra-contrasena-cualquiera", envelope).ok()
        assertEquals(Outcome.Err(LoadError.WrongPassword), cipher.decrypt(key, envelope))
    }
}
