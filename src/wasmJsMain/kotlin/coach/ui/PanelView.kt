package coach.ui

import coach.data.LoadError
import coach.data.Payload
import coach.presentation.Messages
import coach.presentation.PanelController
import coach.presentation.PanelState
import coach.presentation.Presenters
import coach.presentation.weekLabel
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.events.Event

private val REGISTER_ERRORS = setOf(LoadError.NoPasskey, LoadError.WrongPassword, LoadError.TooManyAttempts)

/** Enlaza el estado del controlador con el DOM y los gestos del usuario con sus acciones. */
class PanelView(private val controller: PanelController, private val scope: CoroutineScope) {
    private val boot = required<HTMLElement>("boot")
    private val lock = required<HTMLElement>("lock")
    private val app = required<HTMLElement>("app")
    private val loginButton = required<HTMLButtonElement>("passkey-login")
    private val registerBox = required<HTMLElement>("register-box")
    private val registerForm = required<HTMLFormElement>("register-form")
    private val password = required<HTMLInputElement>("pw")
    private val registerButton = required<HTMLButtonElement>("register")
    private val lockMessage = required<HTMLElement>("lock-msg")
    private val weekSelect = required<HTMLSelectElement>("week")

    /** Estado solo de interfaz: semana elegida en el selector (no global, no persistente). */
    private var selectedWeek: String? = null
    private var payload: Payload? = null

    fun bind() {
        loginButton.addEventListener("click", { _: Event -> controller.login() })
        registerForm.addEventListener("submit", { ev: Event ->
            ev.preventDefault()
            controller.register(password.value)
        })
        required<HTMLButtonElement>("logout").addEventListener("click", { _: Event -> controller.logout() })
        weekSelect.addEventListener("change", { _: Event ->
            selectedWeek = weekSelect.value
            payload?.let(::renderWeek)
        })
        document.addEventListener("scroll", { _: Event -> Tip.hide() })
        scope.launch { controller.state.collect(::render) }
    }

    private fun show(target: HTMLElement) {
        for (e in listOf(boot, lock, app)) e.hidden = e != target
    }

    private fun render(state: PanelState) {
        when (state) {
            PanelState.Loading -> {
                boot.textContent = "Cargando…"
                show(boot)
            }
            is PanelState.Failed -> {
                boot.textContent = Messages.of(state.error)
                show(boot)
            }
            is PanelState.Locked -> {
                document.body?.removeAttribute("data-nivel")
                loginButton.disabled = state.busy
                registerButton.disabled = state.busy
                lockMessage.className = if (state.busy || state.error == null) "lock-msg ok" else "lock-msg"
                lockMessage.textContent = when {
                    state.busy -> "Verificando…"
                    state.error != null -> Messages.of(state.error)
                    else -> ""
                }
                // Sin passkey todavía o contraseña mal: se abre el registro con la contraseña
                if (state.error in REGISTER_ERRORS) registerBox.setAttribute("open", "")
                show(lock)
            }
            is PanelState.Ready -> {
                password.value = ""
                payload = state.payload
                renderReady(state.payload)
                show(app)
            }
        }
    }

    private fun renderReady(p: Payload) {
        val m = p.metricas
        val readiness = Presenters.readiness(m.preparacion)
        readiness.nivel?.let { document.body?.setAttribute("data-nivel", it.name.lowercase()) }
            ?: document.body?.removeAttribute("data-nivel")
        renderHeader(m.fecha, m.semanaActual, p.estado)
        renderReadiness(readiness)
        renderAlerts(Presenters.alerts(m))
        renderTiles(m)
        fillWeekSelect(p)
        renderWeek(p)
        renderE1rm(m)
        renderComposition(Presenters.composition(m.composicion, m.objetivo.fase))
        renderNutrition(m.nutricion?.let(Presenters::nutrition))
        renderAcwr(Presenters.acwr(m.carga, m.umbrales.acwr))
        renderRunning(m)
        renderRecovery(p.diario)
        renderFresh(m.frescura)
    }

    private fun fillWeekSelect(p: Payload) {
        weekSelect.clear()
        for (w in p.metricas.fuerza.semanas.reversed()) {
            val o = el("option", null, weekLabel(w.semana) + if (w.semana == p.metricas.semanaActual) " (en curso)" else "")
                as HTMLOptionElement
            o.value = w.semana
            weekSelect.add(o)
        }
        weekSelect.value = selectedWeek ?: p.metricas.fuerza.semanaCompleta
    }

    private fun renderWeek(p: Payload) {
        val m = p.metricas
        renderMuscles(Presenters.muscles(m, weekSelect.value), m.fuerza.sinMapear.keys)
    }
}
