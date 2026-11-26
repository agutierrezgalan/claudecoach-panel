package coach.data

/**
 * En GitHub Pages (`<owner>.github.io/<repo>/`) los datos cifrados se leen de la rama `data`
 * del mismo repo, así que un fork funciona sin tocar nada. En local, de `./data.json`.
 */
object DataLocation {
    fun url(hostname: String, pathname: String, nowMillis: Long): String {
        val repo = pathname.split('/').firstOrNull { it.isNotEmpty() && !it.contains('.') }
        return if (hostname.endsWith(".github.io") && repo != null) {
            val owner = hostname.substringBefore('.')
            "https://raw.githubusercontent.com/$owner/$repo/data/data.json?t=$nowMillis"
        } else {
            "data.json?t=$nowMillis"
        }
    }
}
