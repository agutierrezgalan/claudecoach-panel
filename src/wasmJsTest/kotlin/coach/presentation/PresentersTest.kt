package coach.presentation

import coach.data.Acwr
import coach.data.Nivel
import coach.data.PayloadCodec
import coach.ok
import coach.fixtures.ContractFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresentersTest {
    private val m = PayloadCodec.decodePayload(ContractFixtures.PLAIN).ok().metricas

    @Test
    fun readinessShowsScoreLevelAndWhy() {
        val r = Presenters.readiness(m.preparacion)
        assertEquals(Nivel.NARANJA, r.nivel)
        assertEquals("41", r.score)
        assertEquals("Naranja · cansado", r.title)
        assertEquals("Recorta un 20-30 % el volumen de fuerza.", r.advice)
        assertEquals(4, r.reasons.size)
        assertTrue(r.reasons.first().startsWith("−20 · HRV"))
    }

    @Test
    fun readinessWithoutDataIsNeutral() {
        val r = Presenters.readiness(m.preparacion.copy(puntuacion = null, nivel = null, motivos = emptyList(), datosSuficientes = false))
        assertEquals(null, r.nivel)
        assertEquals("—", r.score)
        assertEquals("Sin datos suficientes", r.title)
    }

    @Test
    fun everyLevelHasTitleAndAdvice() {
        for (n in Nivel.entries) {
            val r = Presenters.readiness(m.preparacion.copy(nivel = n))
            assertTrue(r.title.isNotBlank() && r.advice.isNotBlank(), "$n")
        }
    }

    @Test
    fun volumeAlertsAreGroupedPerStatus() {
        val alerts = Presenters.alerts(m)
        val volume = alerts.filter { it.text.startsWith("Volumen") }
        assertEquals(1, volume.size)
        assertEquals("Volumen bajo en S47: Pecho, Espalda, Hombro lat., Hombro post., Cuádriceps, Glúteos", volume.single().text)
        assertTrue(alerts.none { it.text.contains(" series en 2026-W") })
        assertEquals(Tone.WARN, alerts.first { it.text.startsWith("Sueño") }.tone)
    }

    @Test
    fun acwrUsesThresholdsFromTheData() {
        val rows = Presenters.acwr(m.carga, m.umbrales.acwr)
        assertEquals(listOf("Carga Garmin", "Km de carrera", "Series de fuerza"), rows.map { it.label })
        assertEquals(Tone.WARN, rows[0].tone)          // 1,38 > 1,3
        assertEquals("1,38 · alta", rows[0].value)
        val strict = Presenters.acwr(m.carga, Acwr(minOk = 0.8, maxOk = 1.1, pico = 1.2))
        assertEquals(Tone.BAD, strict[0].tone)
        assertEquals("1,38 · pico", strict[0].value)
        assertEquals(40.0, rows[0].okStartPct)          // 0,8 / 2,0
        assertEquals(25.0, rows[0].okWidthPct)          // (1,3 - 0,8) / 2,0
    }

    @Test
    fun muscleRowsComparedWithTargets() {
        val rows = Presenters.muscles(m, week = "2026-W47")
        val pecho = rows.first { it.key == "pecho" }
        assertEquals(6.0, pecho.sets)
        assertEquals(MuscleStatus.LOW, pecho.status)
        assertEquals(MuscleStatus.OK, rows.first { it.key == "biceps" }.status)
        assertTrue(rows.none { it.sets == 0.0 && (it.targetLow ?: 0.0) == 0.0 })
    }

    @Test
    fun e1rmChangeTones() {
        assertEquals(Tone.GOOD, Presenters.changeTone(3.7))
        assertEquals(Tone.WARN, Presenters.changeTone(-2.0))
        assertEquals(Tone.BAD, Presenters.changeTone(-5.0))
        assertEquals(Tone.MUTED, Presenters.changeTone(null))
    }
}
