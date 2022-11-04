package no.kartverket.nibas.common

import java.time.LocalDate

data class Lokalid(val value: String)
data class Identifikator(val lokalid: Lokalid, val gyldigFra: LocalDate)
