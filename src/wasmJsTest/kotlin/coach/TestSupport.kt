package coach

import coach.core.Outcome
import kotlin.test.fail

/** Valor de un Ok o fallo del test con el error recibido. */
fun <T, E> Outcome<T, E>.ok(): T = when (this) {
    is Outcome.Ok -> value
    is Outcome.Err -> fail("Se esperaba Ok y llegó Err($error)")
}

/** Error de un Err o fallo del test con el valor recibido. */
fun <T, E> Outcome<T, E>.err(): E = when (this) {
    is Outcome.Ok -> fail("Se esperaba Err y llegó Ok")
    is Outcome.Err -> error
}
