package coach.data

/** Backend de ClaudeCoach (Cloudflare Workers): única fuente de datos de la app. */
object DataLocation {
    const val API = "https://claudecoach-api.claudecoach-alex.workers.dev"

    fun url(path: String): String = API + path
}
