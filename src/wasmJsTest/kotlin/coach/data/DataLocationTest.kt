package coach.data

import kotlin.test.Test
import kotlin.test.assertEquals

class DataLocationTest {
    @Test
    fun githubPagesReadsTheDataBranchOfTheSameRepo() {
        assertEquals(
            "https://raw.githubusercontent.com/ana/coach-panel/data/data.json?t=42",
            DataLocation.url(hostname = "ana.github.io", pathname = "/coach-panel/", nowMillis = 42),
        )
    }

    @Test
    fun forksWorkWithoutChanges() {
        assertEquals(
            "https://raw.githubusercontent.com/otra/mi-panel/data/data.json?t=1",
            DataLocation.url("otra.github.io", "/mi-panel/index.html", 1),
        )
    }

    @Test
    fun localDevelopmentUsesRelativeFile() {
        assertEquals("data.json?t=7", DataLocation.url("127.0.0.1", "/", 7))
        assertEquals("data.json?t=7", DataLocation.url("ana.github.io", "/", 7))
    }
}
