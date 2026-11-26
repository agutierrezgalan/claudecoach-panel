@file:OptIn(ExperimentalWasmJsInterop::class)

package coach.platform

import coach.core.Outcome
import coach.data.Cipher
import coach.data.Envelope
import coach.data.LoadError
import coach.data.SecretKey
import kotlinx.coroutines.await

/** Clave de WebCrypto envuelta para el resto de la app. */
class WebCryptoKey(val key: CryptoKey) : SecretKey

/** Cipher con WebCrypto: PBKDF2-SHA256 -> AES-256-GCM, clave NO extraíble. */
class WebCryptoCipher private constructor(private val subtle: SubtleCrypto) : Cipher {

    override suspend fun deriveKey(password: String, envelope: Envelope): Outcome<SecretKey, LoadError> = try {
        val base = subtle.importKey("raw", utf8Encode(password), "PBKDF2", false, usages("deriveKey")).await<CryptoKey>()
        val key = subtle.deriveKey(
            pbkdf2Params(base64ToBytes(envelope.salt), envelope.iterations),
            base,
            aesGcmKeyType(),
            false,
            usages("decrypt"),
        ).await<CryptoKey>()
        Outcome.Ok(WebCryptoKey(key))
    } catch (e: Throwable) {
        // Solo falla si el sobre trae parámetros imposibles (sal no base64, etc.)
        Outcome.Err(LoadError.Corrupt("derivación de clave: ${e.message.orEmpty().take(80)}"))
    }

    override suspend fun decrypt(key: SecretKey, envelope: Envelope): Outcome<String, LoadError> = try {
        val cryptoKey = (key as? WebCryptoKey)?.key ?: return Outcome.Err(LoadError.WrongPassword)
        val plain = subtle.decrypt(
            aesGcmParams(base64ToBytes(envelope.iv), utf8Encode(envelope.aad)),
            cryptoKey,
            base64ToBytes(envelope.ct),
        ).await<org.khronos.webgl.ArrayBuffer>()
        Outcome.Ok(utf8Decode(plain))
    } catch (e: Throwable) {
        // AES-GCM no distingue: etiqueta inválida = clave equivocada o datos alterados
        Outcome.Err(LoadError.WrongPassword)
    }

    companion object {
        fun create(): Outcome<WebCryptoCipher, LoadError> {
            val subtle = globalCrypto()?.subtle ?: return Outcome.Err(LoadError.NoWebCrypto)
            return Outcome.Ok(WebCryptoCipher(subtle))
        }
    }
}
