@file:OptIn(ExperimentalWasmJsInterop::class)

package coach

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.events.Event
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val scope = MainScope()

fun main() {
    onScrollHideTip()
    byId("logout").addEventListener("click", { _: Event ->
        scope.launch { keyForget().await<JsAny?>(); reloadPage() }
    })
    scope.launch { start() }
}

private suspend fun start() {
    val msg = byId("lock-msg")
    if (!hasWebCrypto()) {
        showLock(); msg.textContent = "Este navegador no permite descifrar (necesita HTTPS)."; return
    }
    val enc = try {
        json.decodeFromString(Encrypted.serializer(), fetchText(dataUrl()).await<JsString>().toString())
    } catch (e: Throwable) {
        showLock()
        msg.textContent = if (e.message?.contains("404") == true)
            "Aún no hay datos publicados: llegarán con la próxima sincronización."
        else "No se pudieron cargar los datos. Reintenta en un momento."
        return
    }
    val saved = keyLoad().await<JsAny?>()
    if (saved != null) {
        try {
            render(decrypt(saved, enc)); return
        } catch (e: Throwable) {
            keyForget().await<JsAny?>()
        }
    }
    showLock()
    byId("lock-form").addEventListener("submit", { ev: Event ->
        ev.preventDefault()
        scope.launch { unlock(enc) }
    })
}

private fun showLock() {
    byId("boot").hidden = true
    byId("lock").hidden = false
}

private suspend fun unlock(enc: Encrypted) {
    val btn = byId("unlock").unsafeCast<HTMLButtonElement>()
    val pw = byId("pw").unsafeCast<HTMLInputElement>()
    val msg = byId("lock-msg")
    btn.disabled = true
    msg.className = "lock-msg ok"
    msg.textContent = "Descifrando…"
    try {
        val key = deriveKey(pw.value, enc.salt, enc.iter).await<JsAny>()
        val data = decrypt(key, enc)
        if (byId("remember").unsafeCast<HTMLInputElement>().checked) keySave(key).await<JsAny?>()
        pw.value = ""
        render(data)
    } catch (e: Throwable) {
        msg.className = "lock-msg"
        msg.textContent = "Contraseña incorrecta."
    } finally {
        btn.disabled = false
    }
}

private suspend fun decrypt(key: JsAny, enc: Encrypted): Payload {
    val text = decryptText(key, enc.iv, enc.ct, enc.aad).await<JsString>().toString()
    return json.decodeFromString(Payload.serializer(), text)
}

// ---------------- render ----------------

private fun render(p: Payload) {
    val m = p.metricas
    renderHeader(p)
    renderAlerts(m)
    renderTiles(m)
    renderMuscles(m)
    renderE1rm(m)
    renderAcwr(m)
    renderRunning(m)
    renderRecovery(p.diario)
    renderFresh(m)
    byId("boot").hidden = true
    byId("lock").hidden = true
    byId("app").hidden = false
}

private fun renderHeader(p: Payload) {
    val m = p.metricas
    byId("subtitle").textContent = "${dayLabel(m.fecha)} · semana ${weekLabel(m.semanaActual)}"
    val chips = byId("sync")
    chips.clear()
    for ((src, name) in listOf("garmin" to "Garmin", "hevy" to "Hevy")) {
        val s = p.estado[src]
        val c = el("span", "chip")
        c.add(el("span", "dot " + when { s == null -> "warn"; s.ok -> "good"; else -> "bad" }))
        c.add(el("span", null, when {
            s == null -> "$name: sin datos"
            s.ok -> "$name ✓ ${s.ultimaOk ?: ""}"
            else -> "$name ✗ (último OK ${s.ultimaOk ?: "nunca"})"
        }))
        chips.add(c)
    }
}

private val VOLUME_ALERT = Regex(""" series en \d{4}-W\d{2} """)
private val SERIOUS = Regex("""pico|por debajo|Sueño|FC en reposo|-\d""")

private fun renderAlerts(m: Metricas) {
    val ul = byId("alerts")
    ul.clear()
    val alerts = m.alertas.filterNot { VOLUME_ALERT.containsMatchIn(it) }.toMutableList()
    val wk = weekLabel(m.fuerza.semanaCompleta)
    for (st in listOf("bajo", "alto")) {
        val ms = m.fuerza.volumen.filterValues { it.estado == st }.keys.map(::muscleName)
        if (ms.isNotEmpty()) alerts.add("Volumen $st en $wk: ${ms.joinToString(", ")}")
    }
    if (alerts.isEmpty()) {
        val li = el("li"); li.add(el("span", "ico", "✅")); li.add(el("span", "none", "Todo en orden esta semana"))
        ul.add(li); return
    }
    for (a in alerts) {
        val li = el("li")
        li.add(el("span", "ico", if (SERIOUS.containsMatchIn(a)) "⚠️" else "ℹ️"))
        li.add(el("span", null, a))
        ul.add(li)
    }
}

private fun tile(label: String, value: String, unit: String, sub: String?, cls: String = ""): HTMLElement {
    val t = el("div", "tile")
    t.add(el("div", "lbl", label))
    val v = el("div", "val", value)
    if (unit.isNotEmpty() && value != "—") v.add(el("small", null, unit))
    t.add(v)
    if (sub != null) t.add(el("div", "sub $cls", sub))
    return t
}

private fun renderTiles(m: Metricas) {
    val host = byId("tiles")
    host.clear()
    val r = m.recuperacion
    val sl = r.sueno
    host.add(tile("Sueño 7 días", f1(sl.h7), " h",
        sl.h28?.let { "28 d: ${f1(it)} h · ${sl.nochesCortas} noches <7 h" },
        if (sl.h7 != null && sl.h7 < 7) "t-warn" else ""))
    val base = r.hrv.base
    val bLo = base?.getOrNull(0); val bHi = base?.getOrNull(1)
    val hrvLow = r.hrv.m7 != null && bLo != null && r.hrv.m7 < bLo
    host.add(tile("HRV 7 días", f1(r.hrv.m7), " ms",
        if (bLo != null) "Base ${f1(bLo)}-${f1(bHi)}${if (hrvLow) " · por debajo" else ""}" else null,
        if (hrvLow) "t-warn" else ""))
    val fc = r.fcReposo
    val rhrUp = fc.m7 != null && fc.m28 != null && fc.m7 - fc.m28 >= 3
    host.add(tile("FC reposo 7 días", f1(fc.m7), " ppm", fc.m28?.let { "28 d: ${f1(it)}" }, if (rhrUp) "t-warn" else ""))
    val runs = m.carrera.semanas
    if (runs.size >= 2) {
        val cur = runs.last(); val prev = runs[runs.size - 2]
        host.add(tile("Km esta semana", f1(cur.km), " km", "${cur.salidas} salidas · semana pasada ${f1(prev.km)} km"))
    }
    val wk = m.fuerza.semanas
    if (wk.size >= 2) {
        val cur = wk.last(); val prev = wk[wk.size - 2]
        host.add(tile("Series esta semana", cur.series.toString(), "", "${cur.sesiones} sesiones · semana pasada ${prev.series}"))
    }
    val b = m.composicion
    if (!b.sinDatos && b.ultimo != null) {
        val k = b.kgSemana
        host.add(tile("Peso medio 7 días", f1(b.peso7), " kg",
            if (k != null) "Ritmo ${if (k > 0) "+" else ""}${f2(k)} kg/sem" else "Último ${f1(b.ultimo.peso)} kg",
            when { k == null -> ""; k < 0 || k > 0.5 -> "t-warn"; else -> "t-good" }))
    } else {
        host.add(tile("Peso", "—", "", "Sin pesajes en Garmin"))
    }
}

private var weekListenerSet = false

private fun renderMuscles(m: Metricas) {
    val sel = byId("week").unsafeCast<HTMLSelectElement>()
    val weeks = m.fuerza.semanas
    if (!weekListenerSet) {
        for (w in weeks.reversed()) {
            val o = el("option", null, weekLabel(w.semana) + if (w.semana == m.semanaActual) " (en curso)" else "")
            o.setAttribute("value", w.semana)
            sel.add(o)
        }
        sel.value = m.fuerza.semanaCompleta
        sel.addEventListener("change", { _: Event -> renderMuscles(m) })
        weekListenerSet = true
    }
    val w = weeks.firstOrNull { it.semana == sel.value } ?: weeks.last()
    val targets = m.fuerza.volumen
    val keys = (targets.keys + w.porMusculo.keys).distinct()
    class Row(val k: String, val v: Double, val lo: Double?, val hi: Double?)
    val rows = keys.map { k ->
        val obj = targets[k]?.objetivo
        Row(k, w.porMusculo[k] ?: 0.0, obj?.getOrNull(0), obj?.getOrNull(1))
    }.filter { it.v > 0 || (it.lo ?: 0.0) > 0 }
    val mx = max(1.0, rows.maxOfOrNull { max(it.v, it.hi ?: 0.0) } ?: 1.0) * 1.05
    val host = byId("muscles")
    host.clear()
    for (r in rows) {
        val row = el("div", "mrow")
        row.add(el("div", "name", muscleName(r.k)))
        val tr = el("div", "track")
        if (r.lo != null && r.hi != null) {
            val band = el("div", "band")
            band.css("left", "${r.lo / mx * 100}%"); band.css("width", "${(r.hi - r.lo) / mx * 100}%")
            tr.add(band)
        }
        val bar = el("div", "bar"); bar.css("width", "${r.v / mx * 100}%"); tr.add(bar)
        row.add(tr)
        val v = el("div", "mval", fmtNum(r.v, 1))
        if (r.lo != null && r.hi != null) {
            val (cls, txt) = when {
                r.v < r.lo -> "t-warn" to "↓ bajo"
                r.v > r.hi -> "t-warn" to "↑ alto"
                else -> "t-good" to "✓"
            }
            v.add(el("span", "st $cls", txt))
            row.setAttribute("title", "Objetivo ${f1(r.lo)}-${f1(r.hi)}")
        }
        row.add(v)
        host.add(row)
    }
    if (m.fuerza.sinMapear.isNotEmpty()) {
        host.add(el("p", "muted small", "Sin músculo asignado: " + m.fuerza.sinMapear.keys.joinToString(", ")))
    }
}

private fun renderE1rm(m: Metricas) {
    val host = byId("e1rm")
    host.clear()
    val weeks = m.fuerza.semanas.map { it.semana }
    if (m.fuerza.e1rm.isEmpty()) {
        host.add(el("div", "empty", "Aún no hay series con peso")); return
    }
    for (e in m.fuerza.e1rm) {
        val row = el("div", "erow")
        val ex = el("div", "ex")
        ex.add(el("div", null, e.ejercicio))
        val last = e.ultima
        ex.add(el("span", null, "e1RM ${f1(e.reciente)} kg" +
            (if (last != null) " · últ. ${f1(last.kg)} kg × ${last.reps ?: "—"}" else "")))
        row.add(ex)
        row.add(sparkline(weeks.map { e.porSemana[it] }))
        val c = e.cambio
        val cls = when { c == null -> "muted"; c >= 0 -> "t-good"; c <= -5 -> "t-bad"; else -> "t-warn" }
        val txt = if (c == null) "—" else (if (c > 0) "▲ " else if (c < 0) "▼ " else "") + fmtNum(abs(c), 1) + " %"
        row.add(el("div", "chg $cls", txt))
        host.add(row)
    }
}

private fun renderAcwr(m: Metricas) {
    val host = byId("acwr")
    host.clear()
    val scaleMax = 2.0
    for ((v, label) in listOf(m.carga.garmin to "Carga Garmin", m.carga.km to "Km de carrera", m.carga.series to "Series de fuerza")) {
        val row = el("div", "arow")
        val head = el("div", "ahead")
        head.add(el("span", null, label))
        val r = v.ratio
        val cls = when { r == null -> "muted"; r > 1.5 -> "t-bad"; r > 1.3 || r < 0.8 -> "t-warn"; else -> "t-good" }
        val word = when { r == null -> "sin datos"; r > 1.5 -> "pico"; r > 1.3 -> "alta"; r < 0.8 -> "baja"; else -> "ok" }
        head.add(el("b", cls, if (r == null) "—" else "${f2(r)} · $word"))
        row.add(head)
        val sc = el("div", "scale")
        val ok = el("div", "ok"); ok.css("left", "${0.8 / scaleMax * 100}%"); ok.css("width", "${0.5 / scaleMax * 100}%"); sc.add(ok)
        if (r != null) {
            val mk = el("div", "mark"); mk.css("left", "${min(r, scaleMax) / scaleMax * 100}%"); sc.add(mk)
        }
        row.add(sc)
        val lbl = el("div", "scale-lbl")
        for (tk in listOf(0.8, 1.3, 1.5)) {
            val s = el("span", null, fmtNum(tk, 1)); s.css("left", "${tk / scaleMax * 100}%"); lbl.add(s)
        }
        row.add(lbl)
        row.add(el("div", "asub", "7 días: ${f1(v.aguda)} · media semanal 28 d: ${f1(v.cronica)}"))
        host.add(row)
    }
}

private fun renderRunning(m: Metricas) {
    columnChart(byId("run-weeks"), m.carrera.semanas.map { w ->
        val cur = w.semana == m.semanaActual
        Column(w.km, weekLabel(w.semana), cur, listOf(
            "Semana ${weekLabel(w.semana)}${if (cur) " (en curso)" else ""}",
            "${f1(w.km)} km · ${w.salidas} salidas",
            "Ritmo ${w.ritmo ?: "—"} /km · larga ${f1(w.larga)} km"))
    }, "Km de carrera por semana")
    val host = byId("runs")
    host.clear()
    val runs = m.carrera.recientes.reversed()
    if (runs.isEmpty()) return
    host.add(table(listOf("Día", "Km", "Ritmo", "FC", "TE"),
        runs.map { listOf(dayLabel(it.fecha), f2(it.km), it.ritmo ?: "—", f1(it.fc), f1(it.te)) }, setOf(1, 3, 4)))
}

private fun renderRecovery(days: List<Dia>) {
    lineChart(byId("ch-sleep"), days, { it.sueno }, " h", "Horas de sueño por día")
    lineChart(byId("ch-hrv"), days, { it.hrv }, " ms", "HRV nocturna", bandLo = { it.hrvLo }, bandHi = { it.hrvHi })
    lineChart(byId("ch-rhr"), days, { it.fcr }, " ppm", "FC en reposo")
    lineChart(byId("ch-weight"), days, { it.peso }, " kg", "Peso", decimals = 2, connectGaps = true)
    val host = byId("daily-table")
    host.clear()
    host.add(table(listOf("Día", "Sueño h", "HRV", "FC rep.", "BB máx", "Peso", "Km", "Gym"),
        days.reversed().map {
            listOf(shortDayLabel(it.d), f2(it.sueno), f1(it.hrv), f1(it.fcr), f1(it.bb), f2(it.peso), f1(it.km),
                if ((it.gym ?: 0) > 0) "✓" else "")
        }, setOf(1, 2, 3, 4, 5, 6)))
}

private val FRESH_NAMES = mapOf(
    "hevy_ultimo_entreno" to "Último entreno (Hevy)", "garmin_ultima_actividad" to "Última actividad (Garmin)",
    "garmin_ultimo_sueno" to "Último sueño", "garmin_ultimo_hrv" to "Último HRV", "garmin_ultimo_peso" to "Último peso",
)

private fun renderFresh(m: Metricas) {
    val ul = byId("fresh")
    ul.clear()
    for ((k, v) in m.frescura) {
        val li = el("li")
        li.add(el("span", "muted", FRESH_NAMES[k] ?: k))
        li.add(el("span", null, if (v != null) dayLabel(v) else "—"))
        ul.add(li)
    }
}
