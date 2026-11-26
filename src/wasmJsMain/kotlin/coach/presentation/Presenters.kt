package coach.presentation

import coach.data.Acwr
import coach.data.Carga
import coach.data.Metricas
import coach.data.Nivel
import coach.data.Preparacion
import coach.data.Ratio

enum class Tone { GOOD, WARN, BAD, MUTED }

enum class MuscleStatus { LOW, OK, HIGH, NONE }

data class ReadinessView(
    val nivel: Nivel?,
    val score: String,
    val title: String,
    val advice: String,
    val reasons: List<String>,
)

data class AlertItem(val text: String, val tone: Tone)

data class AcwrRow(
    val label: String,
    val value: String,
    val tone: Tone,
    val detail: String,
    val markerPct: Double?,
    val okStartPct: Double,
    val okWidthPct: Double,
    val ticks: List<Pair<String, Double>>,
)

data class MuscleRow(
    val key: String,
    val name: String,
    val sets: Double,
    val targetLow: Double?,
    val targetHigh: Double?,
    val status: MuscleStatus,
)

/** Funciones puras: datos del contrato -> modelos de vista. Sin DOM, sin estado. */
object Presenters {
    private val MUSCLES = mapOf(
        "pecho" to "Pecho", "espalda" to "Espalda", "hombro_anterior" to "Hombro ant.",
        "hombro_lateral" to "Hombro lat.", "hombro_posterior" to "Hombro post.", "biceps" to "Bíceps",
        "triceps" to "Tríceps", "antebrazo" to "Antebrazo", "trapecio" to "Trapecio",
        "cuadriceps" to "Cuádriceps", "isquios" to "Isquios", "gluteos" to "Glúteos",
        "aductores" to "Aductores", "gemelos" to "Gemelos", "core" to "Core", "lumbar" to "Lumbar",
    )
    private val LEVELS = mapOf(
        Nivel.VERDE to ("Verde · listo para apretar" to "Buen día para subir cargas."),
        Nivel.AZUL to ("Azul · bien" to "Entrena según el plan."),
        Nivel.AMARILLO to ("Amarillo · atento" to "Mantén cargas, no añadas series."),
        Nivel.NARANJA to ("Naranja · cansado" to "Recorta un 20-30 % el volumen de fuerza."),
        Nivel.ROJO to ("Rojo · recupera" to "Descanso o muy suave; coméntalo con tu entrenador."),
    )
    private val VOLUME_ALERT = Regex(""" series en \d{4}-W\d{2} """)
    private val SERIOUS = Regex("""pico|por debajo|Sueño|FC en reposo|e1RM -|kg/semana""")
    private const val ACWR_SCALE_MIN = 2.0

    fun muscleName(key: String): String = MUSCLES[key] ?: key

    fun readiness(p: Preparacion): ReadinessView {
        val nivel = p.nivel?.takeIf { p.datosSuficientes || p.puntuacion != null }
        val (title, advice) = nivel?.let { LEVELS.getValue(it) }
            ?: ("Sin datos suficientes" to "Hace falta sueño, HRV o FC de los últimos días.")
        return ReadinessView(
            nivel = nivel,
            score = p.puntuacion.es(),
            title = title,
            advice = advice,
            reasons = p.motivos.map { "−${it.resta} · ${it.detalle}" },
        )
    }

    fun alerts(m: Metricas): List<AlertItem> {
        val items = m.alertas.filterNot { VOLUME_ALERT.containsMatchIn(it) }
            .map { AlertItem(it, if (SERIOUS.containsMatchIn(it)) Tone.WARN else Tone.MUTED) }
            .toMutableList()
        val week = weekLabel(m.fuerza.semanaCompleta)
        for ((estado, label) in listOf("bajo" to "bajo", "alto" to "alto")) {
            val names = m.fuerza.volumen.filterValues { it.estado == estado }.keys.map(::muscleName)
            if (names.isNotEmpty()) items += AlertItem("Volumen $label en $week: ${names.joinToString(", ")}", Tone.WARN)
        }
        return items
    }

    fun acwr(carga: Carga, u: Acwr): List<AcwrRow> {
        val scale = maxOf(ACWR_SCALE_MIN, u.pico * 1.25)
        fun pct(v: Double) = v / scale * 100
        fun row(label: String, r: Ratio): AcwrRow {
            val ratio = r.ratio
            val (tone, word) = when {
                ratio == null -> Tone.MUTED to "sin datos"
                ratio > u.pico -> Tone.BAD to "pico"
                ratio > u.maxOk -> Tone.WARN to "alta"
                ratio < u.minOk -> Tone.WARN to "baja"
                else -> Tone.GOOD to "ok"
            }
            return AcwrRow(
                label = label,
                value = if (ratio == null) "—" else "${ratio.es(2)} · $word",
                tone = tone,
                detail = "7 días: ${r.aguda.es()} · media semanal 28 d: ${r.cronica.es()}",
                markerPct = ratio?.let { pct(minOf(it, scale)) },
                okStartPct = pct(u.minOk),
                okWidthPct = pct(u.maxOk - u.minOk),
                ticks = listOf(u.minOk, u.maxOk, u.pico).map { it.es(2) to pct(it) },
            )
        }
        return listOf(row("Carga Garmin", carga.garmin), row("Km de carrera", carga.km), row("Series de fuerza", carga.series))
    }

    fun muscles(m: Metricas, week: String): List<MuscleRow> {
        val w = m.fuerza.semanas.firstOrNull { it.semana == week } ?: m.fuerza.semanas.lastOrNull() ?: return emptyList()
        val keys = (m.fuerza.volumen.keys + w.porMusculo.keys).distinct()
        return keys.map { k ->
            val target = m.fuerza.volumen[k]?.objetivo
            val lo = target?.getOrNull(0)
            val hi = target?.getOrNull(1)
            val sets = w.porMusculo[k] ?: 0.0
            val status = when {
                lo == null || hi == null -> MuscleStatus.NONE
                sets < lo -> MuscleStatus.LOW
                sets > hi -> MuscleStatus.HIGH
                else -> MuscleStatus.OK
            }
            MuscleRow(k, muscleName(k), sets, lo, hi, status)
        }.filter { it.sets > 0 || (it.targetLow ?: 0.0) > 0 }
    }

    fun changeTone(changePct: Double?): Tone = when {
        changePct == null -> Tone.MUTED
        changePct >= 0 -> Tone.GOOD
        changePct <= -5 -> Tone.BAD
        else -> Tone.WARN
    }
}
