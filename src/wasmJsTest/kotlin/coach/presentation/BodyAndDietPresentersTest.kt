package coach.presentation

import coach.data.PayloadCodec
import coach.fixtures.ContractFixtures
import coach.ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BodyAndDietPresentersTest {
    private val m = PayloadCodec.decodePayload(ContractFixtures.PLAIN).ok().metricas

    @Test
    fun contractV2AddsBodyDietAndGoal() {
        assertEquals("definicion", m.objetivo.fase)
        assertEquals("InBodyX", m.composicion.inbody?.ultimo?.equipo)
        assertEquals(2, m.nutricion?.tipos?.size)
        assertEquals(0, m.carrera.diasPista?.sesiones)
    }

    @Test
    fun compositionComparesWithPreviousScanOfSameDevice() {
        val v = assertNotNull(Presenters.composition(m.composicion, m.objetivo.fase))
        assertEquals("InBody 01/11 · InBodyX · vs 01/10 (31 días)", v.title)
        assertEquals("Fase: definición", v.phase)
        assertEquals(
            listOf(
                StatDelta("Peso", "79 kg", "-1", Tone.MUTED),
                StatDelta("Masa muscular", "36,2 kg", "+0,2", Tone.GOOD),
                StatDelta("Grasa · 16,2 %", "12,8 kg", "-1,2", Tone.GOOD),
            ),
            v.stats,
        )
        assertEquals(
            listOf(StatDelta("Brazos", "", "+0,1 kg", Tone.GOOD), StatDelta("Piernas", "", "+0,1 kg", Tone.GOOD),
                StatDelta("Tronco", "", "+0,1 kg", Tone.GOOD)),
            v.segments,
        )
        assertEquals(listOf(36.0, 36.2), v.muscleSeries.map { it.second })
        assertEquals(listOf(14.0, 12.8), v.fatSeries.map { it.second })
        assertNull(v.note)
        assertNull(v.seriesNote)
    }

    @Test
    fun muscleLossInDefinitionIsAWarning() {
        val ib = assertNotNull(m.composicion.inbody)
        val lost = m.composicion.copy(inbody = ib.copy(cambios = ib.cambios.orEmpty() + mapOf("masa_muscular_kg" to -0.6, "magro_piernas_kg" to -0.81)))
        val v = assertNotNull(Presenters.composition(lost, "definicion"))
        assertEquals(StatDelta("Masa muscular", "36,2 kg", "-0,6", Tone.WARN), v.stats[1])
        assertEquals(StatDelta("Piernas", "", "-0,81 kg", Tone.WARN), v.segments[1])
    }

    @Test
    fun differentDeviceAddsACaution() {
        val ib = assertNotNull(m.composicion.inbody)
        val v = assertNotNull(Presenters.composition(m.composicion.copy(inbody = ib.copy(mismoEquipo = false)), "definicion"))
        assertEquals("Equipo distinto al de la medición anterior: compara con cautela.", v.note)
    }

    @Test
    fun weightTileUsesTheRateAgainstThePhase() {
        assertEquals(
            TileView("Peso medio 7 días", "78,5", " kg", "+0,11 kg/sem (Garmin) · fuera de rango", Tone.WARN),
            Presenters.weightTile(m.composicion),
        )
    }

    @Test
    fun withoutRecentWeighInsTheTileShowsTheLatestWeightAndInBodyRate() {
        val c = m.composicion.copy(
            peso7 = null, kgSemana = null,
            ultimo = coach.data.Pesaje("2026-11-04", 81.5, null),
            ritmo = m.composicion.ritmo?.copy(kgSemana = -0.35, fuente = "InBody", enRango = true),
        )
        assertEquals(
            TileView("Último peso · 04/11", "81,5", " kg", "-0,35 kg/sem (InBody) · en rango", Tone.GOOD),
            Presenters.weightTile(c),
        )
        assertEquals("Peso", Presenters.weightTile(coach.data.Composicion(sinDatos = true)).label)
    }

    @Test
    fun weightRateAgainstPhaseTarget() {
        assertEquals(
            "Ritmo de peso +0,11 kg/sem (+0,14 %/sem, Garmin) · objetivo en definición: -1 a -0,25 %/sem",
            Presenters.composition(m.composicion, m.objetivo.fase)?.rate,
        )
    }

    @Test
    fun chartsOnlyUseScansFromTheLatestDevice() {
        val ib = assertNotNull(m.composicion.inbody)
        val old = coach.data.Medicion("2026-09-01", "OtroEquipo", 82.0, 35.0, 16.0, 19.5)
        val v = assertNotNull(Presenters.composition(m.composicion.copy(inbody = ib.copy(serie = listOf(old) + ib.serie)), "definicion"))
        assertEquals(listOf("2026-10-01", "2026-11-01"), v.muscleSeries.map { it.first })
        assertEquals("Gráficas: solo mediciones del InBodyX (mismo equipo)", v.seriesNote)
    }

    @Test
    fun nutritionRowsAreEstimatesPerDayType() {
        val v = assertNotNull(Presenters.nutrition(assertNotNull(m.nutricion)))
        assertEquals(listOf("Gym/descanso", "Carrera"), v.rows.map { it.type })
        val gym = v.rows.first()
        assertEquals(NutritionRow("Gym/descanso", "1400", "1,55", "1,75", "36", Tone.WARN), gym)
        assertEquals(Tone.GOOD, v.rows[1].proteinTone)   // 1,65 ≥ 1,6
        assertEquals("Últimos 7 días: 4 de carrera y 3 de gym o descanso · ~1596 kcal/día según el plan", v.week)
        assertEquals("Plan 2026-11 · valores estimados a partir de las raciones (±15 %) · g/kg sobre 78,6 kg", v.note)
    }

    @Test
    fun trackDaysOnlyWhenThereAreSessions() {
        assertNull(Presenters.trackDays(m.carrera.diasPista))
        val p = coach.data.DiasPista(dias = listOf("mar", "jue"), horaMedia = "18:55", sesiones = 6, ultima = "2026-10-27")
        assertEquals("Pista con tu entrenador: mar y jue (~18:55) · última 27/10", Presenters.trackDays(p))
    }
}
