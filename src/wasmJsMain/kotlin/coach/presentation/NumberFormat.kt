package coach.presentation

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** Formato numérico español (coma decimal), en Kotlin puro para que sea determinista y testeable. */
fun Double?.es(maxDecimals: Int = 1, suffix: String = ""): String {
    if (this == null || isNaN() || isInfinite()) return "—"
    val factor = 10.0.pow(maxDecimals).roundToLong()
    val scaled = (abs(this) * factor).roundToLong()
    val whole = scaled / factor
    val frac = (scaled % factor).toString().padStart(maxDecimals, '0').trimEnd('0')
    val sign = if (this < 0 && scaled != 0L) "-" else ""
    return sign + whole + (if (frac.isEmpty()) "" else ",$frac") + suffix
}

fun Int?.es(suffix: String = ""): String = if (this == null) "—" else "$this$suffix"

fun weekLabel(isoWeek: String): String = "S" + isoWeek.substringAfter("-W")

/** "2026-11-04" -> "04/11" (sin depender de Intl, para poder testearlo). */
fun shortDate(iso: String): String = if (iso.length >= 10) "${iso.substring(8, 10)}/${iso.substring(5, 7)}" else iso
