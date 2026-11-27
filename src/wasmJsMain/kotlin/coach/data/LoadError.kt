package coach.data

/** Todo lo que puede salir mal al cargar el panel, sin texto libre que haya que interpretar. */
sealed interface LoadError {
    /** Error de red o HTTP inesperado; `status` null si ni siquiera hubo respuesta. */
    data class Network(val status: Int?) : LoadError

    /** Contraseña del panel incorrecta al registrar una passkey. */
    data object WrongPassword : LoadError

    /** Demasiados intentos de contraseña: el servidor bloquea un rato. */
    data object TooManyAttempts : LoadError

    /** No hay sesión o ha caducado: hay que entrar con Face ID. */
    data object Unauthorized : LoadError

    /** Aún no hay ninguna passkey registrada: hay que activar Face ID con la contraseña. */
    data object NoPasskey : LoadError

    /** El usuario canceló Face ID (o se agotó el tiempo). */
    data object PasskeyCancelled : LoadError

    /** El servidor o el navegador rechazaron la passkey. */
    data object PasskeyRejected : LoadError

    /** Este navegador no admite passkeys. */
    data object NoPasskeySupport : LoadError

    /** El contrato de datos tiene una versión que esta app no sabe leer. */
    data class UnsupportedVersion(val found: Int, val supported: IntRange) : LoadError

    /** Los datos no tienen el formato esperado. */
    data class Corrupt(val reason: String) : LoadError
}
