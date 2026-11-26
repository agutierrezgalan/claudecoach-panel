package coach

import coach.core.Outcome
import coach.data.PanelRepository
import coach.platform.FetchSource
import coach.platform.IndexedDbKeyStore
import coach.platform.WebCryptoCipher
import coach.presentation.PanelController
import coach.ui.PanelView
import kotlinx.coroutines.MainScope

/** Raíz de composición: aquí, y solo aquí, se eligen las implementaciones concretas. */
fun main() {
    val scope = MainScope()
    val controller = when (val cipher = WebCryptoCipher.create()) {
        is Outcome.Err -> PanelController.unavailable(cipher.error)
        is Outcome.Ok -> PanelController(PanelRepository(FetchSource(), cipher.value, IndexedDbKeyStore()), scope)
    }
    PanelView(controller, scope).bind()
    controller.start()
}
