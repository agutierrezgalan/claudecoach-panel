package coach.presentation

import coach.core.Outcome
import coach.data.LoadError
import coach.data.PanelRepository
import coach.data.SessionToken
import coach.fixtures.ContractFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
class PanelControllerTest {
    private val api = FakeApi()
    private val passkeys = FakePasskeys()
    private val sessions = FakeSessionStore()

    private fun TestScope.started(): PanelController =
        PanelController(PanelRepository(api, passkeys, sessions), this).also { it.start(); advanceUntilIdle() }

    @Test
    fun startsLoading() = runTest {
        assertEquals(PanelState.Loading, PanelController(PanelRepository(api, passkeys, sessions), this).state.value)
    }

    @Test
    fun withoutSessionAsksToLogIn() = runTest {
        assertEquals(PanelState.Locked(error = null), started().state.value)
        assertEquals(emptyList(), api.calls) // ni siquiera pide datos sin sesión
    }

    @Test
    fun validSessionOpensDirectly() = runTest {
        sessions.stored = SessionToken("tok-ok")
        val ready = assertIs<PanelState.Ready>(started().state.value)
        assertEquals("2026-11-26", ready.payload.metricas.fecha)
        assertEquals(0, passkeys.gets) // sin Face ID mientras dure la sesión
    }

    @Test
    fun expiredSessionIsForgottenAndAsksToLogIn() = runTest {
        sessions.stored = SessionToken("caducado")
        assertEquals(PanelState.Locked(error = null), started().state.value)
        assertEquals(null, sessions.stored)
    }

    @Test
    fun faceIdLoginOpensAndKeepsTheSession() = runTest {
        val c = started()
        c.login(); advanceUntilIdle()
        assertIs<PanelState.Ready>(c.state.value)
        assertEquals(SessionToken("tok-ok"), sessions.stored)
        assertEquals(listOf("loginOptions", "login", "panel"), api.calls)
    }

    @Test
    fun cancelledFaceIdStaysLockedWithAMessage() = runTest {
        passkeys.cancel = true
        val c = started()
        c.login(); advanceUntilIdle()
        assertEquals(PanelState.Locked(error = LoadError.PasskeyCancelled), c.state.value)
        assertEquals(null, sessions.stored)
    }

    @Test
    fun withoutAPasskeyYetAsksToRegister() = runTest {
        api.hasPasskey = false
        val c = started()
        c.login(); advanceUntilIdle()
        assertEquals(PanelState.Locked(error = LoadError.NoPasskey), c.state.value)
    }

    @Test
    fun registeringWithThePasswordCreatesThePasskeyAndLogsIn() = runTest {
        api.hasPasskey = false
        val c = started()
        c.register("buena"); advanceUntilIdle()
        assertIs<PanelState.Ready>(c.state.value)
        assertEquals(1, passkeys.creates)
        assertEquals(listOf("registrationOptions", "register", "loginOptions", "login", "panel"), api.calls)
    }

    @Test
    fun wrongPasswordOrTooManyAttemptsStayLocked() = runTest {
        val c = started()
        c.register("mala"); advanceUntilIdle()
        assertEquals(PanelState.Locked(error = LoadError.WrongPassword), c.state.value)
        assertEquals(0, passkeys.creates)
        api.tooManyAttempts = true
        c.register("buena"); advanceUntilIdle()
        assertEquals(PanelState.Locked(error = LoadError.TooManyAttempts), c.state.value)
    }

    @Test
    fun networkErrorsAreTypedFailures() = runTest {
        sessions.stored = SessionToken("tok-ok")
        api.panelResult = Outcome.Err(LoadError.Network(503))
        assertEquals(PanelState.Failed(LoadError.Network(503)), started().state.value)
    }

    @Test
    fun unsupportedVersionIsAFailure() = runTest {
        sessions.stored = SessionToken("tok-ok")
        api.panelResult = Outcome.Ok(ContractFixtures.PLAIN.replaceFirst("\"schema_version\": 2", "\"schema_version\": 3"))
        assertEquals(PanelState.Failed(LoadError.UnsupportedVersion(3, 2..2)), started().state.value)
    }

    @Test
    fun logoutClosesTheSessionOnTheServerAndHere() = runTest {
        sessions.stored = SessionToken("tok-ok")
        val c = started()
        c.logout(); advanceUntilIdle()
        assertEquals(PanelState.Locked(error = null), c.state.value)
        assertEquals(null, sessions.stored)
        assertEquals("logout", api.calls.last())
    }

    @Test
    fun unavailablePlatformFailsFast() = runTest {
        assertEquals(PanelState.Failed(LoadError.NoPasskeySupport),
            PanelController.unavailable(LoadError.NoPasskeySupport).state.value)
    }
}
