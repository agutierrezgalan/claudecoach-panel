package coach.presentation

import coach.data.LoadError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessagesTest {
    @Test
    fun everyErrorHasAHumanMessage() {
        val errors = listOf(
            LoadError.NoData, LoadError.Network(503), LoadError.Network(null), LoadError.WrongPassword,
            LoadError.UnsupportedVersion(3, 2..2), LoadError.UnsupportedVersion(1, 2..2),
            LoadError.Corrupt("x"), LoadError.NoWebCrypto,
        )
        errors.forEach { assertTrue(Messages.of(it).isNotBlank(), "$it") }
    }

    @Test
    fun versionMessagesSayWhatToDo() {
        assertTrue(Messages.of(LoadError.UnsupportedVersion(3, 2..2)).contains("Recarga"))
        assertTrue(Messages.of(LoadError.UnsupportedVersion(1, 2..2)).contains("próxima sincronización"))
    }

    @Test
    fun networkMessageShowsStatus() {
        assertEquals("No se pudieron cargar los datos (HTTP 503). Reintenta en un momento.", Messages.of(LoadError.Network(503)))
    }

    @Test
    fun numberFormatIsSpanishAndStable() {
        assertEquals("1,38", 1.38.es(2))
        assertEquals("6", 6.0.es())
        assertEquals("-2,5", (-2.5).es())
        assertEquals("0", (-0.04).es())
        assertEquals("—", (null as Double?).es())
        assertEquals("119,5 kg", 119.5.es(1, " kg"))
        assertEquals("S47", weekLabel("2026-W47"))
    }
}
