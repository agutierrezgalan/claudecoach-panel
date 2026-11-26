package coach.ui

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

// Utilidades DOM tipadas. Los datos SIEMPRE entran como textContent o atributos, nunca como HTML.

const val SVG_NS = "http://www.w3.org/2000/svg"

/** Elemento obligatorio de index.html con su tipo comprobado. */
inline fun <reified T : Element> required(id: String): T =
    document.getElementById(id) as? T ?: error("index.html: falta #$id o no es ${T::class.simpleName}")

fun el(tag: String, cls: String? = null, text: String? = null): HTMLElement {
    val e = document.createElement(tag) as HTMLElement
    if (cls != null) e.className = cls
    if (text != null) e.textContent = text
    return e
}

fun Element.add(child: Element): Element {
    appendChild(child)
    return child
}

fun Element.clear() {
    while (true) removeChild(firstChild ?: break)
}

fun svg(tag: String, attrs: Map<String, Any> = emptyMap(), parent: Element? = null, text: String? = null): Element {
    val e = document.createElementNS(SVG_NS, tag)
    for ((k, v) in attrs) e.setAttribute(k, v.toString())
    if (text != null) e.textContent = text
    parent?.appendChild(e)
    return e
}

fun HTMLElement.css(prop: String, value: String) = style.setProperty(prop, value)
