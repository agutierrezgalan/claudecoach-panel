package coach.presentation

import coach.data.LoadError

/** Texto para el usuario de cada error tipado (exhaustivo: un error nuevo no compila sin su texto). */
object Messages {
    fun of(error: LoadError): String = when (error) {
        LoadError.NoData -> "Aún no hay datos publicados: llegarán con la próxima sincronización."
        is LoadError.Network -> if (error.status == null) "Sin conexión. Reintenta en un momento."
        else "No se pudieron cargar los datos (HTTP ${error.status}). Reintenta en un momento."
        LoadError.WrongPassword -> "Contraseña incorrecta."
        is LoadError.UnsupportedVersion ->
            if (error.found > error.supported.last) "Los datos son de una versión más nueva (v${error.found}). Recarga la app."
            else "Los datos son de una versión anterior (v${error.found}); se actualizarán en la próxima sincronización."
        is LoadError.Corrupt -> "Los datos publicados no tienen el formato esperado."
        LoadError.NoWebCrypto -> "Este navegador no permite descifrar (hace falta HTTPS)."
    }
}
