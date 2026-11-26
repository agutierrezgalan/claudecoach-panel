package coach.data

import coach.err
import coach.ok
import coach.fixtures.ContractFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PayloadCodecTest {
    @Test
    fun decodesThePythonContractFixture() {
        val p = PayloadCodec.decodePayload(ContractFixtures.PLAIN).ok()
        assertEquals(2, p.schemaVersion)
        assertEquals("2026-11-26", p.metricas.fecha)
        assertEquals(56, p.diario.size)
        val prep = p.metricas.preparacion
        assertEquals(41, prep.puntuacion)
        assertEquals(Nivel.NARANJA, prep.nivel)
        assertEquals(listOf("hrv", "fc_reposo", "sueno", "carga"), prep.motivos.map { it.senal })
        assertEquals(1.5, p.metricas.umbrales.acwr.pico)
        assertTrue(p.metricas.fuerza.e1rm.isNotEmpty())
        assertEquals(false, p.estado.getValue("hevy").ok)
    }

    @Test
    fun rejectsUnknownSchemaVersions() {
        for (v in listOf(1, 3)) {
            val json = ContractFixtures.PLAIN.replaceFirst("\"schema_version\": 2", "\"schema_version\": $v")
            val err = PayloadCodec.decodePayload(json).err()
            assertEquals(LoadError.UnsupportedVersion(found = v, supported = PayloadCodec.SUPPORTED_SCHEMA), err)
        }
    }

    @Test
    fun missingVersionIsTreatedAsV1AndRejected() {
        val err = PayloadCodec.decodePayload("""{"metricas": {}}""").err()
        assertEquals(LoadError.UnsupportedVersion(found = 1, supported = PayloadCodec.SUPPORTED_SCHEMA), err)
    }

    @Test
    fun garbageIsCorruptNotACrash() {
        assertIs<LoadError.Corrupt>(PayloadCodec.decodePayload("no es json").err())
        assertIs<LoadError.Corrupt>(PayloadCodec.decodeEnvelope("{}").err())
    }

    @Test
    fun decodesTheEncryptedEnvelope() {
        val env = PayloadCodec.decodeEnvelope(ContractFixtures.ENCRYPTED).ok()
        assertEquals(600_000, env.iterations)
        assertEquals("claudecoach-v1", env.aad)
    }

    @Test
    fun nullReadinessLevelWhenNotEnoughData() {
        val json = ContractFixtures.PLAIN
            .replaceFirst("\"puntuacion\": 41", "\"puntuacion\": null")
            .replaceFirst("\"nivel\": \"naranja\"", "\"nivel\": null")
        val p = PayloadCodec.decodePayload(json).ok()
        assertEquals(null, p.metricas.preparacion.nivel)
    }

    @Test
    fun statusEntriesThatAreNotSourcesDoNotBreakTheLoad() {
        // status.json guarda también apuntes sin "ok" (p. ej. la cuota de Hevy): no deben dar Corrupt
        val json = ContractFixtures.PLAIN.replaceFirst(
            "\"estado\": {",
            "\"estado\": {\"hevy_cuota\": {\"fecha\": \"2026-11-26\", \"intentos\": 1, \"ultimo_ok\": null},",
        )
        val p = PayloadCodec.decodePayload(json).ok()
        assertEquals(true, p.estado["garmin"]?.ok)
        assertEquals(false, p.estado["hevy_cuota"]?.ok)
    }
}
