package coach.platform

import coach.core.Outcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Usa el fetch real de la plataforma (Node aquí): valida que las opciones de la petición son aceptadas. */
class FetchSourceTest {
    @Test
    fun readsTheBodyWithARealFetch() = runTest {
        val source = FetchSource(url = { "data:application/json,%7B%22ok%22%3Atrue%7D" })
        assertEquals(Outcome.Ok("""{"ok":true}"""), source.fetch())
    }
}
