package coach.presentation

import coach.core.Outcome
import coach.data.LoadError
import coach.data.PanelRepository
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
    private val source = FakeSource()
    private val cipher = FakeCipher()
    private val keys = FakeKeyStore()

    private fun TestScope.started(): PanelController =
        PanelController(PanelRepository(source, cipher, keys), this).also { it.start(); advanceUntilIdle() }

    @Test
    fun startsLoading() = runTest {
        val c = PanelController(PanelRepository(source, cipher, keys), this)
        assertEquals(PanelState.Loading, c.state.value)
    }

    @Test
    fun withoutStoredKeyAsksForPassword() = runTest {
        assertEquals(PanelState.Locked(error = null), started().state.value)
    }

    @Test
    fun storedKeyOpensDirectly() = runTest {
        keys.stored = cipher.keyFor("buena")
        val ready = assertIs<PanelState.Ready>(started().state.value)
        assertEquals("2026-11-26", ready.payload.metricas.fecha)
    }

    @Test
    fun staleStoredKeyIsForgottenAndAsksForPassword() = runTest {
        keys.stored = cipher.keyFor("antigua")
        assertEquals(PanelState.Locked(error = null), started().state.value)
        assertEquals(1, keys.clears)
        assertEquals(null, keys.stored)
    }

    @Test
    fun noDataYetIsATypedFailure() = runTest {
        source.result = Outcome.Err(LoadError.NoData)
        assertEquals(PanelState.Failed(LoadError.NoData), started().state.value)
    }

    @Test
    fun networkErrorsCarryTheStatus() = runTest {
        source.result = Outcome.Err(LoadError.Network(status = 503))
        assertEquals(PanelState.Failed(LoadError.Network(503)), started().state.value)
    }

    @Test
    fun wrongPasswordStaysLockedAndSavesNothing() = runTest {
        val c = started()
        c.unlock("mala", remember = true); advanceUntilIdle()
        assertEquals(PanelState.Locked(error = LoadError.WrongPassword), c.state.value)
        assertEquals(0, keys.saves)
    }

    @Test
    fun rightPasswordOpensAndRemembersOnlyIfAsked() = runTest {
        val c = started()
        c.unlock("buena", remember = false); advanceUntilIdle()
        assertIs<PanelState.Ready>(c.state.value)
        assertEquals(0, keys.saves)
        c.forget(); advanceUntilIdle()
        c.unlock("buena", remember = true); advanceUntilIdle()
        assertIs<PanelState.Ready>(c.state.value)
        assertEquals(1, keys.saves)
    }

    @Test
    fun unsupportedVersionAfterDecryptIsAFailure() = runTest {
        cipher.plaintext = ContractFixtures.PLAIN.replaceFirst("\"schema_version\": 2", "\"schema_version\": 3")
        val c = started()
        c.unlock("buena", remember = true); advanceUntilIdle()
        assertEquals(PanelState.Failed(LoadError.UnsupportedVersion(3, 2..2)), c.state.value)
        assertEquals(0, keys.saves)
    }

    @Test
    fun forgetClearsKeyAndLocks() = runTest {
        keys.stored = cipher.keyFor("buena")
        val c = started()
        c.forget(); advanceUntilIdle()
        assertEquals(PanelState.Locked(error = null), c.state.value)
        assertEquals(null, keys.stored)
    }

    @Test
    fun unavailablePlatformFailsFast() = runTest {
        assertEquals(PanelState.Failed(LoadError.NoWebCrypto), PanelController.unavailable(LoadError.NoWebCrypto).state.value)
    }

    @Test
    fun corruptEnvelopeIsAFailure() = runTest {
        source.result = Outcome.Ok("<html>no es json</html>")
        assertIs<LoadError.Corrupt>(assertIs<PanelState.Failed>(started().state.value).error)
        assertEquals(1, source.calls)
        assertEquals(ContractFixtures.PASSWORD.isNotEmpty(), true)
    }
}
