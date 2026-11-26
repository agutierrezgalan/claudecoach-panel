package coach.data

import coach.core.Outcome
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Decodifica el sobre y el payload, comprobando las versiones del contrato. */
object PayloadCodec {
    /** Versiones del payload (`schema_version`) que esta app sabe leer. */
    val SUPPORTED_SCHEMA: IntRange = 2..2
    private const val ENVELOPE_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun decodeEnvelope(text: String): Outcome<Envelope, LoadError> {
        val env = try {
            json.decodeFromString(Envelope.serializer(), text)
        } catch (e: SerializationException) {
            return Outcome.Err(LoadError.Corrupt("sobre: ${e.message.orEmpty().take(120)}"))
        } catch (e: IllegalArgumentException) {
            return Outcome.Err(LoadError.Corrupt("sobre: ${e.message.orEmpty().take(120)}"))
        }
        return if (env.v == ENVELOPE_VERSION) Outcome.Ok(env)
        else Outcome.Err(LoadError.Corrupt("sobre v${env.v} desconocido"))
    }

    fun decodePayload(text: String): Outcome<Payload, LoadError> = try {
        val root = json.parseToJsonElement(text).jsonObject
        // Sin campo: payload anterior al contrato v2
        val version = root["schema_version"]?.jsonPrimitive?.int ?: 1
        if (version !in SUPPORTED_SCHEMA) {
            Outcome.Err(LoadError.UnsupportedVersion(found = version, supported = SUPPORTED_SCHEMA))
        } else {
            Outcome.Ok(json.decodeFromJsonElement(Payload.serializer(), root))
        }
    } catch (e: SerializationException) {
        Outcome.Err(LoadError.Corrupt(e.message.orEmpty().take(160)))
    } catch (e: IllegalArgumentException) {
        Outcome.Err(LoadError.Corrupt(e.message.orEmpty().take(160)))
    }
}
