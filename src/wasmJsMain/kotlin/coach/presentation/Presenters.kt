package coach.presentation

import coach.data.Acwr
import coach.data.Carga
import coach.data.Composicion
import coach.data.DiasPista
import coach.data.Metricas
import coach.data.Nivel
import coach.data.Nutricion
import coach.data.Preparacion
import coach.data.Ratio
import kotlin.math.abs

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

data class TileView(val label: String, val value: String, val unit: String, val sub: String?, val tone: Tone?)

/** Un valor con su cambio frente a la medición anterior (delta vacío si no hay con qué comparar). */
data class StatDelta(val label: String, val value: String, val delta: String, val tone: Tone)

data class CompositionView(
    val title: String,
    val phase: String?,
    val stats: List<StatDelta>,
    val segments: List<StatDelta>,
    val rate: String?,
    val muscleSeries: List<Pair<String, Double>>,
    val fatSeries: List<Pair<String, Double>>,
    val note: String?,
    val seriesNote: String?,
)

data class NutritionRow(
    val type: String,
    val kcal: String,
    val proteinPerKg: String,
    val carbsPerKg: String,
    val fat: String,
    val proteinTone: Tone,
)

data class NutritionView(val rows: List<NutritionRow>, val week: String?, val note: String)

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
    private val PHASES = mapOf(
        "definicion" to "definición", "recomposicion" to "recomposición",
        "volumen" to "volumen", "mantenimiento" to "mantenimiento",
    )
    private val SEGMENTS = listOf("magro_brazos_kg" to "Brazos", "magro_piernas_kg" to "Piernas", "magro_tronco_kg" to "Tronco")
    private val DAY_TYPES = mapOf("gym_o_descanso" to "Gym/descanso", "carrera" to "Carrera")
    private const val MUSCLE_LOSS_KG = -0.5   // misma regla que la alerta de ClaudeCoach
    private const val MUSCLE_STABLE_KG = -0.2 // por debajo de esto es ruido de medida
    private const val PROTEIN_MIN_G_KG = 1.6

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

    fun phaseName(fase: String): String = PHASES[fase] ?: fase

    /** Última medición InBody frente a la anterior; tonos según la fase (en definición: conservar músculo). */
    fun composition(c: Composicion, fase: String?): CompositionView? {
        val ib = c.inbody ?: return null
        val u = ib.ultimo ?: return null
        val prev = ib.anterior
        val comparable = prev != null && ib.mismoEquipo != false
        fun d(key: String) = ib.cambios?.get(key)
        fun tone(delta: Double?, rule: (Double) -> Tone) = if (comparable && delta != null) rule(delta) else Tone.MUTED
        val muscle = d("masa_muscular_kg")
        val fat = d("masa_grasa_kg")
        val stats = listOf(
            StatDelta("Peso", u.peso.es(1, " kg"), signed(d("peso_kg")), Tone.MUTED),
            StatDelta("Masa muscular", u.musculo.es(1, " kg"), signed(muscle), tone(muscle, ::muscleTone)),
            StatDelta("Grasa" + u.grasaPct?.let { " · ${it.es(1, " %")}" }.orEmpty(), u.grasa.es(1, " kg"), signed(fat), tone(fat) { fatTone(it, fase) }),
        )
        val segments = SEGMENTS.mapNotNull { (key, label) ->
            d(key)?.let { StatDelta(label, "", signed(it, 2) + " kg", tone(it, ::muscleTone)) }
        }
        val same = ib.serie.filter { it.equipo == u.equipo }
        val title = buildString {
            append("InBody ").append(shortDate(u.fecha))
            u.equipo?.let { append(" · ").append(it) }
            prev?.let { append(" · vs ").append(shortDate(it.fecha)) }
            ib.diasEntre?.takeIf { prev != null }?.let { append(" ($it días)") }
        }
        return CompositionView(
            title = title,
            phase = fase?.let { "Fase: ${phaseName(it)}" },
            stats = stats,
            segments = segments,
            rate = weightRate(c),
            muscleSeries = same.mapNotNull { m -> m.musculo?.let { m.fecha to it } },
            fatSeries = same.mapNotNull { m -> m.grasa?.let { m.fecha to it } },
            note = when {
                prev == null -> "Primera medición: aún no hay con qué comparar."
                ib.mismoEquipo == false -> "Equipo distinto al de la medición anterior: compara con cautela."
                else -> null
            },
            seriesNote = if (same.size < ib.serie.size && u.equipo != null) "Gráficas: solo mediciones del ${u.equipo} (mismo equipo)" else null,
        )
    }

    private fun muscleTone(delta: Double): Tone = when {
        delta <= MUSCLE_LOSS_KG -> Tone.WARN
        delta >= MUSCLE_STABLE_KG -> Tone.GOOD
        else -> Tone.MUTED
    }

    private fun fatTone(delta: Double, fase: String?): Tone = when (fase) {
        "definicion", "recomposicion" -> when { delta < 0 -> Tone.GOOD; delta > 0.3 -> Tone.WARN; else -> Tone.MUTED }
        "volumen" -> if (delta > 1.0) Tone.WARN else Tone.MUTED
        else -> if (abs(delta) > 1.0) Tone.WARN else Tone.MUTED
    }

    private fun weightRate(c: Composicion): String? {
        val r = c.ritmo ?: return null
        val kg = r.kgSemana ?: return null
        val source = listOfNotNull(r.pctSemana?.let { signed(it, 2) + " %/sem" }, r.fuente).joinToString(", ")
        val target = r.objetivo.takeIf { it.size == 2 && r.fase != null }
            ?.let { " · objetivo en ${phaseName(r.fase.orEmpty())}: ${it[0].es(2)} a ${it[1].es(2)} %/sem" }.orEmpty()
        return "Ritmo de peso ${signed(kg, 2)} kg/sem" + (if (source.isEmpty()) "" else " ($source)") + target
    }

    /** Tarjeta de peso: media de 7 días si hay pesajes recientes; si no, el último peso (Garmin o InBody). */
    fun weightTile(c: Composicion): TileView {
        val r = c.ritmo
        val rate = r?.kgSemana?.let { "${signed(it, 2)} kg/sem" + (r.fuente?.let { f -> " ($f)" }.orEmpty()) }
            ?: c.kgSemana?.let { "${signed(it, 2)} kg/sem" }
        val range = when (r?.enRango) { true -> "en rango"; false -> "fuera de rango"; null -> null }
        val sub = listOfNotNull(rate, range).joinToString(" · ").ifEmpty { null }
        val tone = when (r?.enRango) { true -> Tone.GOOD; false -> Tone.WARN; null -> null }
        if (c.peso7 != null) return TileView("Peso medio 7 días", c.peso7.es(), " kg", sub, tone)
        val garmin = c.ultimo?.takeIf { it.peso != null }?.let { Triple(it.fecha, it.peso, "Último peso") }
        val inbody = c.inbody?.ultimo?.takeIf { it.peso != null }?.let { Triple(it.fecha, it.peso, "Peso InBody") }
        val latest = listOfNotNull(garmin, inbody).maxByOrNull { it.first }
            ?: return TileView("Peso", "—", "", "Sin pesajes en Garmin ni InBody", null)
        return TileView("${latest.third} · ${shortDate(latest.first)}", latest.second.es(), " kg", sub, tone)
    }

    /** Plan de dieta del mes por tipo de día. Valores ESTIMADOS: se muestran como tales. */
    fun nutrition(n: Nutricion): NutritionView? {
        if (n.tipos.isEmpty()) return null
        val keys = DAY_TYPES.keys.filter { it in n.tipos } + n.tipos.keys.filterNot { it in DAY_TYPES }
        val rows = keys.map { k ->
            val t = n.tipos.getValue(k)
            val protein = n.proteina[k]
            NutritionRow(
                type = DAY_TYPES[k] ?: k.replace('_', ' '),
                kcal = t.kcal.es(0),
                proteinPerKg = protein.es(2),
                carbsPerKg = n.hidratos[k].es(2),
                fat = t.grasa.es(0),
                proteinTone = when { protein == null -> Tone.MUTED; protein >= PROTEIN_MIN_G_KG -> Tone.GOOD; else -> Tone.WARN },
            )
        }
        val week = if (n.dias.isEmpty()) null else
            "Últimos 7 días: ${n.dias["carrera"] ?: 0} de carrera y ${n.dias["gym_o_descanso"] ?: 0} de gym o descanso" +
                n.kcalMedia?.let { " · ~${it.es(0)} kcal/día según el plan" }.orEmpty()
        val note = listOfNotNull(
            "Plan ${n.plan ?: "del mes"}",
            "valores estimados a partir de las raciones (±15 %)".takeIf { n.estimado },
            n.pesoRef?.kg?.let { "g/kg sobre ${it.es()} kg" },
        ).joinToString(" · ")
        return NutritionView(rows, week, note)
    }

    /** Días habituales de pista con el entrenador; null si no hay sesiones recientes. */
    fun trackDays(p: DiasPista?): String? {
        if (p == null || p.sesiones == 0 || p.dias.isEmpty()) return null
        val days = if (p.dias.size == 1) p.dias[0] else p.dias.dropLast(1).joinToString(", ") + " y " + p.dias.last()
        return "Pista con tu entrenador: $days" + p.horaMedia?.let { " (~$it)" }.orEmpty() +
            p.ultima?.let { " · última ${shortDate(it)}" }.orEmpty()
    }

    private fun signed(v: Double?, decimals: Int = 1): String {
        if (v == null) return ""
        val s = v.es(decimals)
        return if (v > 0 && s != "0") "+$s" else s
    }
}
