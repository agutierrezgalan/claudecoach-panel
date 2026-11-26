package coach.presentation

import coach.data.LoadError
import coach.data.Payload

/** Estado completo de la pantalla: la UI solo lo pinta. */
sealed interface PanelState {
    data object Loading : PanelState

    /** Pide contraseña; `error` explica el intento anterior; `busy` mientras se descifra. */
    data class Locked(val error: LoadError?, val busy: Boolean = false) : PanelState

    data class Ready(val payload: Payload) : PanelState

    data class Failed(val error: LoadError) : PanelState
}
