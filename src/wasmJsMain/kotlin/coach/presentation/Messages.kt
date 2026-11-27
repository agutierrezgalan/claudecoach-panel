package coach.presentation

import coach.data.LoadError

/** Texto para el usuario de cada error tipado (exhaustivo: un error nuevo no compila sin su texto). */
object Messages {
    fun of(error: LoadError): String = when (error) {
        is LoadError.Network -> if (error.status == null) "Sin conexión. Reintenta en un momento."
        else "No se pudieron cargar los datos (HTTP ${error.status}). Reintenta en un momento."
        LoadError.WrongPassword -> "Contraseña incorrecta."
        LoadError.TooManyAttempts -> "Demasiados intentos. Prueba dentro de una hora."
        LoadError.Unauthorized -> "La sesión ha caducado. Entra de nuevo con Face ID."
        LoadError.NoPasskey -> "Este panel aún no tiene Face ID activado: actívalo con la contraseña."
        LoadError.PasskeyCancelled -> "Face ID cancelado. Pulsa «Entrar con Face ID» para reintentar."
        LoadError.PasskeyRejected -> "No se pudo verificar la passkey. Vuelve a intentarlo."
        LoadError.NoPasskeySupport -> "Este navegador no admite Face ID / passkeys (hace falta Safari o Chrome recientes)."
        is LoadError.UnsupportedVersion ->
            if (error.found > error.supported.last) "Los datos son de una versión más nueva (v${error.found}). Recarga la app."
            else "Los datos son de una versión anterior (v${error.found}); se actualizarán en la próxima sincronización."
        is LoadError.Corrupt -> "Los datos recibidos no tienen el formato esperado."
    }
}
