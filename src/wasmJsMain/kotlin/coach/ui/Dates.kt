package coach.ui

import coach.platform.dateLabelEs

fun dayLabel(iso: String): String = dateLabelEs(iso, long = true)

fun shortDayLabel(iso: String): String = dateLabelEs(iso, long = false)
