package coach.presentation

import coach.core.Outcome
import coach.data.Envelope
import coach.data.LoadError
import coach.data.PanelRepository
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
    private var envelope: Envelope? = null

    fun start() = act { repo ->
        _state.value = PanelState.Loading
        when (val env = repo.envelope()) {
            is Outcome.Err -> _state.value = PanelState.Failed(env.error)
            is Outcome.Ok -> {
                envelope = env.value
                _state.value = when (val opened = repo.openWithStoredKey(env.value)) {
                    null -> PanelState.Locked(error = null)
                    else -> opened.toState()
                }
            }
        }
    }

    fun unlock(password: String, remember: Boolean) = act { repo ->
        val env = envelope ?: return@act
        _state.value = PanelState.Locked(error = null, busy = true)
        _state.value = repo.unlock(env, password, remember).toState()
    }

    fun forget() = act { repo ->
        repo.forget()
        if (envelope != null) _state.value = PanelState.Locked(error = null)
    }

    private fun act(block: suspend (PanelRepository) -> Unit) {
        val r = repo ?: return
        scope?.launch { block(r) }
    }

    private fun Outcome<coach.data.Payload, LoadError>.toState(): PanelState = when (this) {
        is Outcome.Ok -> PanelState.Ready(value)
        is Outcome.Err -> if (error == LoadError.WrongPassword) PanelState.Locked(error) else PanelState.Failed(error)
    }

    companion object {
        /** Plataforma sin lo imprescindible (p. ej. WebCrypto): estado final, sin acciones. */
        fun unavailable(error: LoadError): PanelController = PanelController(null, null, PanelState.Failed(error))
    }
}
