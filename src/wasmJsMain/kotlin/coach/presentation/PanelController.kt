package coach.presentation

import coach.core.Outcome
import coach.data.LoadError
import coach.data.PanelRepository
import coach.data.Payload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Máquina de estados del panel (flujo unidireccional: acciones -> repositorio -> estado). */
class PanelController private constructor(
    private val repo: PanelRepository?,
    private val scope: CoroutineScope?,
    initial: PanelState,
) {
    constructor(repo: PanelRepository, scope: CoroutineScope) : this(repo, scope, PanelState.Loading)

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<PanelState> = _state.asStateFlow()

    fun start() = act { repo ->
        _state.value = PanelState.Loading
        _state.value = repo.openWithSession()?.toState() ?: PanelState.Locked(error = null)
    }

    /** Entrar con Face ID. */
    fun login() = act { repo ->
        _state.value = PanelState.Locked(error = null, busy = true)
        _state.value = repo.loginWithPasskey().toState()
    }

    /** Activar Face ID en este dispositivo (la primera vez), con la contraseña del panel. */
    fun register(password: String) = act { repo ->
        _state.value = PanelState.Locked(error = null, busy = true)
        _state.value = repo.registerPasskey(password).toState()
    }

    fun logout() = act { repo ->
        repo.logout()
        _state.value = PanelState.Locked(error = null)
    }

    private fun act(block: suspend (PanelRepository) -> Unit) {
        val r = repo ?: return
        scope?.launch { block(r) }
    }

    private fun Outcome<Payload, LoadError>.toState(): PanelState = when (this) {
        is Outcome.Ok -> PanelState.Ready(value)
        is Outcome.Err -> if (error in RETRYABLE_ON_LOCK) PanelState.Locked(error) else PanelState.Failed(error)
    }

    companion object {
        /** Errores que se resuelven desde la pantalla de acceso (reintentar Face ID o activar el dispositivo). */
        private val RETRYABLE_ON_LOCK: Set<LoadError> = setOf(
            LoadError.Unauthorized, LoadError.NoPasskey, LoadError.PasskeyCancelled, LoadError.PasskeyRejected,
            LoadError.WrongPassword, LoadError.TooManyAttempts,
        )

        /** Plataforma sin lo imprescindible (p. ej. sin passkeys): estado final, sin acciones. */
        fun unavailable(error: LoadError): PanelController = PanelController(null, null, PanelState.Failed(error))
    }
}
