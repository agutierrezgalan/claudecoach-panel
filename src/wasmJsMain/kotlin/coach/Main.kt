package coach

import coach.core.Outcome
import coach.data.PanelRepository
import coach.platform.HttpPanelApi
import coach.platform.IndexedDbSessionStore
import coach.platform.WebAuthnPasskeys
import coach.presentation.PanelController
import coach.ui.PanelView
import kotlinx.coroutines.MainScope

/** Raíz de composición: aquí, y solo aquí, se eligen las implementaciones concretas. */
fun main() {
    val scope = MainScope()
    val controller = when (val passkeys = WebAuthnPasskeys.create()) {
        is Outcome.Err -> PanelController.unavailable(passkeys.error)
        is Outcome.Ok -> PanelController(PanelRepository(HttpPanelApi(), passkeys.value, IndexedDbSessionStore()), scope)
    }
    PanelView(controller, scope).bind()
    controller.start()
}
