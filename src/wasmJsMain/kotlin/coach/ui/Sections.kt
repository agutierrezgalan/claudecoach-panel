package coach.ui

import coach.data.Dia
import coach.data.Estado
import coach.data.Metricas
import coach.presentation.AcwrRow
import coach.presentation.AlertItem
import coach.presentation.MuscleRow
import coach.presentation.MuscleStatus
import coach.presentation.Presenters
import coach.presentation.ReadinessView
import coach.presentation.Tone
import coach.presentation.es
import coach.presentation.weekLabel
import org.w3c.dom.HTMLElement
import kotlin.math.abs

// Cada sección: modelo de vista -> DOM. Sin lógica de negocio (eso vive en Presenters).

private fun Tone.css(): String = when (this) {
    Tone.GOOD -> "t-good"
    Tone.WARN -> "t-warn"
    Tone.BAD -> "t-bad"
    Tone.MUTED -> "muted"
}

fun renderHeader(fecha: String, semana: String, estado: Map<String, Estado>) {
    required<HTMLElement>("subtitle").textContent = "${dayLabel(fecha)} · semana ${weekLabel(semana)}"
    val chips = required<HTMLElement>("sync")
    chips.clear()
    for ((src, name) in listOf("garmin" to "Garmin", "hevy" to "Hevy")) {
        val s = estado[src]
        val chip = el("span", "chip")
        chip.add(el("span", "dot " + when { s == null -> "warn"; s.ok -> "good"; else -> "bad" }))
        chip.add(el("span", null, when {
            s == null -> "$name: sin datos"
            s.ok -> "$name ✓ ${s.ultimaOk.orEmpty()}"
            else -> "$name ✗ (último OK ${s.ultimaOk ?: "nunca"})"
        }))
        chips.add(chip)
    }
}

fun renderReadiness(r: ReadinessView) {
    required<HTMLElement>("r-score").textContent = r.score
    required<HTMLElement>("r-title").textContent = r.title
    required<HTMLElement>("r-advice").textContent = r.advice
    val ul = required<HTMLElement>("r-reasons")
    ul.clear()
    r.reasons.forEach { ul.add(el("li", null, it)) }
    if (r.reasons.isEmpty() && r.nivel != null) ul.add(el("li", null, "Todas las señales en orden"))
}

fun renderAlerts(items: List<AlertItem>) {
    val ul = required<HTMLElement>("alerts")
    ul.clear()
    if (items.isEmpty()) {
        val li = el("li")
        li.add(el("span", "ico", "✓"))
        li.add(el("span", "t-good", "Todo en orden esta semana"))
        ul.add(li)
        return
    }
    for (a in items) {
        val li = el("li")
        li.add(el("span", "ico " + a.tone.css(), if (a.tone == Tone.MUTED) "i" else "!"))
        li.add(el("span", null, a.text))
        ul.add(li)
    }
}

private fun tile(label: String, value: String, unit: String, sub: String?, tone: Tone? = null): HTMLElement {
    val t = el("div", "tile")
    t.add(el("div", "lbl", label))
    val v = el("div", "val", value)
    if (unit.isNotEmpty() && value != "—") v.add(el("small", null, unit))
    t.add(v)
    if (sub != null) t.add(el("div", "sub " + (tone?.css() ?: ""), sub))
    return t
}

fun renderTiles(m: Metricas) {
    val host = required<HTMLElement>("tiles")
    host.clear()
    val r = m.recuperacion
    val sl = r.sueno
    host.add(tile("Sueño 7 días", sl.h7.es(), " h",
        sl.h28?.let { "28 d: ${it.es()} h · ${sl.nochesCortas} noches <7 h" },
        if (sl.h7 != null && sl.h7 < 7) Tone.WARN else null))
    val lo = r.hrv.base?.getOrNull(0)
    val hi = r.hrv.base?.getOrNull(1)
    val hrvLow = r.hrv.m7 != null && lo != null && r.hrv.m7 < lo
    host.add(tile("HRV 7 días", r.hrv.m7.es(), " ms",
        lo?.let { "Base ${it.es()}-${hi.es()}${if (hrvLow) " · por debajo" else ""}" },
        if (hrvLow) Tone.WARN else null))
    val fc = r.fcReposo
    val rhrUp = fc.m7 != null && fc.m28 != null && fc.m7 - fc.m28 >= 3
    host.add(tile("FC reposo 7 días", fc.m7.es(), " ppm", fc.m28?.let { "28 d: ${it.es()}" }, if (rhrUp) Tone.WARN else null))
    val runs = m.carrera.semanas
    if (runs.size >= 2) {
        val cur = runs.last()
        host.add(tile("Km esta semana", cur.km.es(), " km", "${cur.salidas} salidas · semana pasada ${runs[runs.size - 2].km.es()} km"))
    }
    val wk = m.fuerza.semanas
    if (wk.size >= 2) {
        val cur = wk.last()
        host.add(tile("Series esta semana", cur.series.toString(), "", "${cur.sesiones} sesiones · semana pasada ${wk[wk.size - 2].series}"))
    }
    val b = m.composicion
    if (!b.sinDatos && b.ultimo != null) {
        val k = b.kgSemana
        host.add(tile("Peso medio 7 días", b.peso7.es(), " kg",
            if (k != null) "Ritmo ${if (k > 0) "+" else ""}${k.es(2)} kg/sem" else "Último ${b.ultimo.peso.es()} kg",
            when { k == null -> null; k < 0 || k > 0.5 -> Tone.WARN; else -> Tone.GOOD }))
    } else {
        host.add(tile("Peso", "—", "", "Sin pesajes en Garmin"))
    }
}

fun renderMuscles(rows: List<MuscleRow>, unmapped: Collection<String>) {
    val max = maxOf(1.0, rows.maxOfOrNull { maxOf(it.sets, it.targetHigh ?: 0.0) } ?: 1.0) * 1.05
    val host = required<HTMLElement>("muscles")
    host.clear()
    for (r in rows) {
        val row = el("div", "mrow")
        row.add(el("div", "name", r.name))
        val track = el("div", "track")
        if (r.targetLow != null && r.targetHigh != null) {
            val band = el("div", "band")
            band.css("left", "${r.targetLow / max * 100}%")
            band.css("width", "${(r.targetHigh - r.targetLow) / max * 100}%")
            track.add(band)
            row.setAttribute("title", "Objetivo ${r.targetLow.es()}-${r.targetHigh.es()}")
        }
        val bar = el("div", "bar")
        bar.css("width", "${r.sets / max * 100}%")
        track.add(bar)
        row.add(track)
        val value = el("div", "mval", r.sets.es())
        when (r.status) {
            MuscleStatus.LOW -> value.add(el("span", "st t-warn", "↓ bajo"))
            MuscleStatus.HIGH -> value.add(el("span", "st t-warn", "↑ alto"))
            MuscleStatus.OK -> value.add(el("span", "st t-good", "✓"))
            MuscleStatus.NONE -> Unit
        }
        row.add(value)
        host.add(row)
    }
    if (unmapped.isNotEmpty()) host.add(el("p", "muted small", "Sin músculo asignado: " + unmapped.joinToString(", ")))
}

fun renderE1rm(m: Metricas) {
    val host = required<HTMLElement>("e1rm")
    host.clear()
    if (m.fuerza.e1rm.isEmpty()) {
        host.add(el("div", "empty", "Aún no hay series con peso"))
        return
    }
    val weeks = m.fuerza.semanas.map { it.semana }
    for (e in m.fuerza.e1rm) {
        val row = el("div", "erow")
        val ex = el("div", "ex")
        ex.add(el("div", null, e.ejercicio))
        val last = e.ultima
        ex.add(el("span", null, "e1RM ${e.reciente.es(1, " kg")}" +
            (if (last != null) " · últ. ${last.kg.es(1, " kg")} × ${last.reps ?: "—"}" else "")))
        row.add(ex)
        row.add(sparkline(weeks.map { e.porSemana[it] }))
        val c = e.cambio
        val arrow = when { c == null -> ""; c > 0 -> "▲ "; c < 0 -> "▼ "; else -> "" }
        row.add(el("div", "chg " + Presenters.changeTone(c).css(), if (c == null) "—" else arrow + abs(c).es(1, " %")))
        host.add(row)
    }
}

fun renderAcwr(rows: List<AcwrRow>) {
    val host = required<HTMLElement>("acwr")
    host.clear()
    for (r in rows) {
        val row = el("div", "arow")
        val head = el("div", "ahead")
        head.add(el("span", null, r.label))
        head.add(el("b", r.tone.css(), r.value))
        row.add(head)
        val scale = el("div", "scale")
        val ok = el("div", "ok")
        ok.css("left", "${r.okStartPct}%")
        ok.css("width", "${r.okWidthPct}%")
        scale.add(ok)
        r.markerPct?.let { pct ->
            val mark = el("div", "mark")
            mark.css("left", "$pct%")
            scale.add(mark)
        }
        row.add(scale)
        val labels = el("div", "scale-lbl")
        for ((text, pct) in r.ticks) {
            val s = el("span", null, text)
            s.css("left", "$pct%")
            labels.add(s)
        }
        row.add(labels)
        row.add(el("div", "asub", r.detail))
        host.add(row)
    }
}

fun renderRunning(m: Metricas) {
    columnChart(required("run-weeks"), m.carrera.semanas.map { w ->
        val current = w.semana == m.semanaActual
        Column(w.km, weekLabel(w.semana), current, listOf(
            "Semana ${weekLabel(w.semana)}${if (current) " (en curso)" else ""}",
            "${w.km.es()} km · ${w.salidas} salidas",
            "Ritmo ${w.ritmo ?: "—"} /km · larga ${w.larga.es()} km",
        ))
    }, "Km de carrera por semana")
    val host = required<HTMLElement>("runs")
    host.clear()
    val runs = m.carrera.recientes.reversed()
    if (runs.isEmpty()) return
    host.add(table(listOf("Día", "Km", "Ritmo", "FC", "TE"),
        runs.map { listOf(dayLabel(it.fecha), it.km.es(2), it.ritmo ?: "—", it.fc.es(), it.te.es()) }, setOf(1, 3, 4)))
}

fun renderRecovery(days: List<Dia>) {
    lineChart(required("ch-sleep"), days, { it.sueno }, " h", "Horas de sueño por día")
    lineChart(required("ch-hrv"), days, { it.hrv }, " ms", "HRV nocturna", bandLo = { it.hrvLo }, bandHi = { it.hrvHi })
    lineChart(required("ch-rhr"), days, { it.fcr }, " ppm", "FC en reposo")
    lineChart(required("ch-weight"), days, { it.peso }, " kg", "Peso", decimals = 2, connectGaps = true)
    val host = required<HTMLElement>("daily-table")
    host.clear()
    host.add(table(listOf("Día", "Sueño h", "HRV", "FC rep.", "BB máx", "Peso", "Km", "Gym"),
        days.reversed().map {
            listOf(shortDayLabel(it.d), it.sueno.es(2), it.hrv.es(), it.fcr.es(), it.bb.es(), it.peso.es(2), it.km.es(),
                if ((it.gym ?: 0) > 0) "✓" else "")
        }, setOf(1, 2, 3, 4, 5, 6)))
}

private val FRESH_NAMES = mapOf(
    "hevy_ultimo_entreno" to "Último entreno (Hevy)", "garmin_ultima_actividad" to "Última actividad (Garmin)",
    "garmin_ultimo_sueno" to "Último sueño", "garmin_ultimo_hrv" to "Último HRV", "garmin_ultimo_peso" to "Último peso",
)

fun renderFresh(frescura: Map<String, String?>) {
    val ul = required<HTMLElement>("fresh")
    ul.clear()
    for ((k, v) in frescura) {
        val li = el("li")
        li.add(el("span", "muted", FRESH_NAMES[k] ?: k))
        li.add(el("span", null, if (v != null) dayLabel(v) else "—"))
        ul.add(li)
    }
}
