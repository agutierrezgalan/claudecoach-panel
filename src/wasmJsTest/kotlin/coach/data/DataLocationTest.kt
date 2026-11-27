package coach.data

import kotlin.test.Test
import kotlin.test.assertEquals

class DataLocationTest {
    @Test
    fun theApiIsTheCloudflareBackend() {
        assertEquals("https://claudecoach-api.claudecoach-alex.workers.dev", DataLocation.API)
        assertEquals("https://claudecoach-api.claudecoach-alex.workers.dev/v1/panel", DataLocation.url("/v1/panel"))
    }
}
