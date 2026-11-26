@file:OptIn(ExperimentalWasmJsInterop::class)

package coach

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

// Utilidades DOM. Los datos SIEMPRE entran como textContent o atributos, nunca como HTML.

const val SVG_NS = "http://www.w3.org/2000/svg"

fun byId(id: String): HTMLElement = document.getElementById(id)!!.unsafeCast<HTMLElement>()

fun el(tag: String, cls: String? = null, text: String? = null): HTMLElement {
    val e = document.createElement(tag).unsafeCast<HTMLElement>()
    if (cls != null) e.className = cls
    if (text != null) e.textContent = text
    return e
}

fun Element.add(child: Element): Element {
    appendChild(child)
    return child
}

fun Element.clear() {
    while (true) {
        val c = firstChild ?: break
        removeChild(c)
    }
}

fun svg(tag: String, attrs: Map<String, Any> = emptyMap(), parent: Element? = null, text: String? = null): Element {
    val e = document.createElementNS(SVG_NS, tag)
    for ((k, v) in attrs) e.setAttribute(k, v.toString())
    if (text != null) e.textContent = text
    parent?.appendChild(e)
    return e
}

fun HTMLElement.css(prop: String, value: String) {
    style.setProperty(prop, value)
}

// ---------- formato ----------
fun f1(v: Double?): String = if (v == null || v.isNaN()) "—" else fmtNum(v, 1)
fun f2(v: Double?): String = if (v == null || v.isNaN()) "—" else fmtNum(v, 2)
fun f1u(v: Double?, unit: String): String = if (v == null) "—" else f1(v) + unit
fun weekLabel(w: String): String = "S" + w.substringAfter("-W")

val MUSCLE_NAMES = mapOf(
    "pecho" to "Pecho", "espalda" to "Espalda", "hombro_anterior" to "Hombro ant.",
    "hombro_lateral" to "Hombro lat.", "hombro_posterior" to "Hombro post.", "biceps" to "Bíceps",
    "triceps" to "Tríceps", "antebrazo" to "Antebrazo", "trapecio" to "Trapecio",
    "cuadriceps" to "Cuádriceps", "isquios" to "Isquios", "gluteos" to "Glúteos",
    "aductores" to "Aductores", "gemelos" to "Gemelos", "core" to "Core", "lumbar" to "Lumbar",
)

fun muscleName(k: String) = MUSCLE_NAMES[k] ?: k
