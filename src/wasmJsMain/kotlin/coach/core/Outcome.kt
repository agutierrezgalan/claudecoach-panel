package coach.core

/** Resultado tipado: éxito o error de dominio (sin excepciones que crucen capas). */
sealed interface Outcome<out T, out E> {
    data class Ok<out T>(val value: T) : Outcome<T, Nothing>
    data class Err<out E>(val error: E) : Outcome<Nothing, E>
}

inline fun <T, E, R> Outcome<T, E>.map(transform: (T) -> R): Outcome<R, E> = when (this) {
    is Outcome.Ok -> Outcome.Ok(transform(value))
    is Outcome.Err -> this
}

inline fun <T, E, R> Outcome<T, E>.flatMap(transform: (T) -> Outcome<R, E>): Outcome<R, E> = when (this) {
    is Outcome.Ok -> transform(value)
    is Outcome.Err -> this
}
