package coach.ui

import coach.data.Dia
import coach.presentation.es
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Gráficos SVG propios: una serie por gráfico, rejilla tenue, tooltip al tocar/pasar.

object Tip {
    private val tip: HTMLElement by lazy { required<HTMLElement>("tip") }

    fun show(x: Double, y: Double, lines: List<String>) {
        tip.clear()
        lines.forEachIndexed { i, l ->
            val d = el("div", null, l)
            if (i == 0) d.css("font-weight", "600")
            tip.add(d)
        }
        tip.hidden = false
        val w = tip.offsetWidth.toDouble()
        val left = min(max(x, w / 2 + 8), window.innerWidth - w / 2 - 8)
        tip.css("left", "${left}px")
        tip.css("top", "${max(y, tip.offsetHeight + 16.0)}px")
    }

    fun hide() {
        tip.hidden = true
    }
}

private fun Event.clientX(): Double = (this as? MouseEvent)?.clientX?.toDouble() ?: 0.0

private class Point(val i: Int, val day: String, val y: Double)

fun lineChart(
    host: HTMLElement,
    days: List<Dia>,
    value: (Dia) -> Double?,
    unit: String,
    label: String,
    decimals: Int = 1,
    bandLo: ((Dia) -> Double?)? = null,
    bandHi: ((Dia) -> Double?)? = null,
    connectGaps: Boolean = false,
) = lineChart(host, days, Dia::d, value, unit, label, decimals, bandLo, bandHi, connectGaps)

/** Serie de puntos (fecha, valor) a intervalos regulares, p. ej. mediciones InBody. */
fun pointsChart(host: HTMLElement, points: List<Pair<String, Double>>, unit: String, label: String) =
    lineChart(host, points, { it.first }, { it.second }, unit, label, connectGaps = true)

private fun <T> lineChart(
    host: HTMLElement,
    days: List<T>,
    date: (T) -> String,
    value: (T) -> Double?,
    unit: String,
    label: String,
    decimals: Int = 1,
    bandLo: ((T) -> Double?)? = null,
    bandHi: ((T) -> Double?)? = null,
    connectGaps: Boolean = false,
) {
    host.clear()
    val pts = days.mapIndexedNotNull { i, d -> value(d)?.let { Point(i, date(d), it) } }
    if (pts.size < 2 || days.size < 2) {
        host.add(el("div", "empty", "Aún no hay datos suficientes"))
        return
    }
    val w = 340.0; val h = 150.0; val l = 30.0; val r = 8.0; val t = 10.0; val b = 20.0
    val vals = pts.map { it.y }.toMutableList()
    if (bandLo != null && bandHi != null) days.forEach { d -> bandLo(d)?.let(vals::add); bandHi(d)?.let(vals::add) }
    var lo = vals.min(); var hi = vals.max()
    val pad = ((hi - lo) * 0.15).takeIf { it > 0 } ?: 1.0
    lo -= pad; hi += pad
    val n = days.size
    fun x(i: Int) = l + i.toDouble() / (n - 1) * (w - l - r)
    fun y(v: Double) = t + (1 - (v - lo) / (hi - lo)) * (h - t - b)

    val s = svg("svg", mapOf("viewBox" to "0 0 $w $h", "role" to "img", "aria-label" to label))
    val ax = svg("g", mapOf("class" to "axis"), s)
    for (tv in listOf(lo + pad, (lo + hi) / 2, hi - pad)) {
        svg("line", mapOf("class" to "gridline", "x1" to l, "x2" to w - r, "y1" to y(tv), "y2" to y(tv)), ax)
        svg("text", mapOf("x" to l - 6, "y" to y(tv) + 4, "text-anchor" to "end"), ax, tv.es(1))
    }
    for (i in listOf(0, (n - 1) / 2, n - 1)) {
        val anchor = when (i) { 0 -> "start"; n - 1 -> "end"; else -> "middle" }
        svg("text", mapOf("x" to x(i), "y" to h - 4, "text-anchor" to anchor), ax, shortDayLabel(date(days[i])))
    }
    if (bandLo != null && bandHi != null) {
        // (índice, inferior, superior) solo de los días con banda completa
        val band = days.indices.mapNotNull { i ->
            val lo = bandLo(days[i]) ?: return@mapNotNull null
            val hi = bandHi(days[i]) ?: return@mapNotNull null
            Triple(i, lo, hi)
        }
        if (band.size > 1) {
            val top = band.mapIndexed { k, (i, _, hiV) -> (if (k == 0) "M" else "L") + "${x(i)} ${y(hiV)}" }
            val bot = band.reversed().map { (i, loV, _) -> "L${x(i)} ${y(loV)}" }
            svg("path", mapOf("class" to "series-band", "d" to (top + bot).joinToString("") + "Z"), s)
        }
    }
    val path = StringBuilder()
    var prev = -10
    for (p in pts) {
        path.append(if (p.i == prev + 1 || (connectGaps && prev >= 0)) "L" else "M").append("${x(p.i)} ${y(p.y)}")
        prev = p.i
    }
    svg("path", mapOf("class" to "series", "d" to path.toString()), s)
    val present = pts.map { it.i }.toSet()
    for (p in pts) if (connectGaps || ((p.i - 1) !in present && (p.i + 1) !in present)) {
        svg("circle", mapOf("class" to "pt", "cx" to x(p.i), "cy" to y(p.y), "r" to 3), s)
    }
    val cross = svg("line", mapOf("class" to "cross", "y1" to t, "y2" to h - b, "visibility" to "hidden"), s)
    val hot = svg("circle", mapOf("class" to "hot", "r" to 5, "visibility" to "hidden"), s)
    val hit = svg("rect", mapOf("x" to l, "y" to 0, "width" to w - l - r, "height" to h, "fill" to "transparent"), s)
    val move: (Event) -> Unit = { ev ->
        val box = s.getBoundingClientRect()
        val sx = (ev.clientX() - box.left) / box.width * w
        val best = pts.minBy { abs(x(it.i) - sx) }
        cross.setAttribute("x1", "${x(best.i)}"); cross.setAttribute("x2", "${x(best.i)}")
        cross.setAttribute("visibility", "visible")
        hot.setAttribute("cx", "${x(best.i)}"); hot.setAttribute("cy", "${y(best.y)}")
        hot.setAttribute("visibility", "visible")
        val lines = mutableListOf(dayLabel(best.day), best.y.es(decimals, unit))
        if (bandLo != null && bandHi != null) {
            val dlo = bandLo(days[best.i]); val dhi = bandHi(days[best.i])
            if (dlo != null && dhi != null) lines.add("Rango base ${dlo.es()}-${dhi.es()}")
        }
        Tip.show(box.left + x(best.i) / w * box.width, box.top + y(best.y) / h * box.height, lines)
    }
    hit.addEventListener("pointermove", move)
    hit.addEventListener("pointerdown", move)
    hit.addEventListener("pointerleave", { _: Event ->
        cross.setAttribute("visibility", "hidden"); hot.setAttribute("visibility", "hidden"); Tip.hide()
    })
    host.add(s)
}

class Column(val v: Double, val label: String, val current: Boolean, val tip: List<String>)

fun columnChart(host: HTMLElement, items: List<Column>, label: String) {
    host.clear()
    if (items.none { it.v > 0 }) {
        host.add(el("div", "empty", "Sin carreras registradas"))
        return
    }
    val w = 340.0; val h = 150.0; val l = 4.0; val r = 4.0; val t = 18.0; val b = 20.0
    val mx = items.maxOf { it.v } * 1.1
    val slot = (w - l - r) / items.size
    val bw = min(30.0, slot * 0.6)
    fun y(v: Double) = t + (1 - v / mx) * (h - t - b)
    val s = svg("svg", mapOf("viewBox" to "0 0 $w $h", "role" to "img", "aria-label" to label))
    svg("line", mapOf("class" to "baseline", "x1" to l, "x2" to w - r, "y1" to h - b, "y2" to h - b), s)
    val ax = svg("g", mapOf("class" to "axis"), s)
    items.forEachIndexed { i, it ->
        val cx = l + slot * i + slot / 2
        if (it.v > 0) {
            val top = y(it.v)
            val rr = min(4.0, h - b - top)
            val x0 = cx - bw / 2; val x1 = cx + bw / 2
            val d = "M$x0 ${h - b}V${top + rr}Q$x0 $top ${x0 + rr} ${top}H${x1 - rr}Q$x1 $top $x1 ${top + rr}V${h - b}Z"
            svg("path", mapOf("class" to if (it.current) "col cur" else "col", "d" to d), s)
            svg("text", mapOf("class" to "col-lbl", "x" to cx, "y" to top - 5), s, it.v.es(1))
        }
        svg("text", mapOf("x" to cx, "y" to h - 5, "text-anchor" to "middle"), ax, it.label)
        val hit = svg("rect", mapOf("x" to cx - slot / 2, "y" to 0, "width" to slot, "height" to h, "fill" to "transparent"), s)
        val show: (Event) -> Unit = { _ ->
            val bx = hit.getBoundingClientRect()
            Tip.show(bx.left + bx.width / 2, bx.top + y(it.v) / h * bx.height, it.tip)
        }
        hit.addEventListener("pointerenter", show)
        hit.addEventListener("pointerdown", show)
        hit.addEventListener("pointerleave", { _: Event -> Tip.hide() })
    }
    host.add(s)
}

fun sparkline(values: List<Double?>): Element {
    val s = svg("svg", mapOf("viewBox" to "0 0 84 28", "aria-hidden" to "true"))
    val v = values.filterNotNull()
    if (v.size < 2) return s
    val lo = v.min(); val span = (v.max() - lo).takeIf { it > 0 } ?: 1.0
    fun x(i: Int) = 3 + i.toDouble() / (values.size - 1) * 78
    fun y(value: Double) = 4 + (1 - (value - lo) / span) * 20
    val d = StringBuilder()
    values.forEachIndexed { i, value -> if (value != null) d.append(if (d.isEmpty()) "M" else "L").append("${x(i)} ${y(value)}") }
    svg("path", mapOf("class" to "spark", "d" to d.toString()), s)
    val last = values.indexOfLast { it != null }
    val lastValue = values.getOrNull(last) ?: return s
    svg("circle", mapOf("class" to "spark-dot", "cx" to x(last), "cy" to y(lastValue), "r" to 3), s)
    return s
}

fun table(headers: List<String>, rows: List<List<String>>, numeric: Set<Int>): Element {
    val t = el("table")
    val tr = el("tr")
    headers.forEachIndexed { i, hd -> tr.add(el("th", if (i in numeric) "n" else null, hd)) }
    t.add(el("thead")).add(tr)
    val tb = el("tbody")
    for (r in rows) {
        val row = el("tr")
        r.forEachIndexed { i, c -> row.add(el("td", if (i in numeric) "n" else null, c)) }
        tb.add(row)
    }
    t.add(tb)
    return t
}

fun onScrollHideTip() {
    document.addEventListener("scroll", { _: Event -> Tip.hide() })
}
