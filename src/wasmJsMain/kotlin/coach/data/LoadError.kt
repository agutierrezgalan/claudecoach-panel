package coach.data

/** Todo lo que puede salir mal al cargar el panel, sin texto libre que haya que interpretar. */
sealed interface LoadError {
    /** Aún no hay datos publicados (404 en la rama `data`). */
    data object NoData : LoadError

    /** Error de red o HTTP distinto de 404; `status` null si ni siquiera hubo respuesta. */
    data class Network(val status: Int?) : LoadError

    /** La clave no descifra los datos (contraseña incorrecta o clave guardada antigua). */
    data object WrongPassword : LoadError

    /** El contrato de datos tiene una versión que esta app no sabe leer. */
    data class UnsupportedVersion(val found: Int, val supported: IntRange) : LoadError

    /** Los datos no tienen el formato esperado. */
    data class Corrupt(val reason: String) : LoadError

    /** El navegador no ofrece WebCrypto (p. ej. fuera de HTTPS). */
    data object NoWebCrypto : LoadError
}
