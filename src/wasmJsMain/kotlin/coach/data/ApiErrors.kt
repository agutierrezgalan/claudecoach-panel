package coach.data

/** Llamadas a la API, para interpretar cada código HTTP en su contexto. */
enum class ApiCall { REGISTER, REGISTER_VERIFY, LOGIN_OPTIONS, LOGIN, PANEL }

/** Códigos HTTP y errores del navegador -> errores tipados. */
object ApiErrors {
    fun of(status: Int?, call: ApiCall): LoadError = when {
        status == null -> LoadError.Network(null)
        status == 429 -> LoadError.TooManyAttempts
        status == 401 && call == ApiCall.REGISTER -> LoadError.WrongPassword
        status == 401 -> LoadError.Unauthorized
        status == 404 && call == ApiCall.LOGIN_OPTIONS -> LoadError.NoPasskey
        status == 400 && call == ApiCall.REGISTER_VERIFY -> LoadError.PasskeyRejected
        else -> LoadError.Network(status)
    }

    /** Nombre del DOMException de navigator.credentials. */
    fun ofPasskey(name: String): LoadError = when (name) {
        "NotAllowedError", "AbortError" -> LoadError.PasskeyCancelled
        "NotSupportedError", "SecurityError" -> LoadError.NoPasskeySupport
        else -> LoadError.PasskeyRejected
    }
}
