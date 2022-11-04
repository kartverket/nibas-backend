package no.kartverket.nibas.common

import java.time.LocalDate

fun LocalDate.inThePast(): Boolean = this.isBefore(LocalDate.now())
