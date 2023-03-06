package no.kartverket.nibas.common

import java.io.Serializable
import java.time.LocalDate

data class Lokalid(val value: String)

/**
 * Forretningsnøkkel.
 * Kombinasjonen av lokalid (uuid) + gyldighetsdato skal alltid bare returnere en rad.
 */
data class Identifikator(val lokalid: Lokalid, val gyldighetsdato: LocalDate = LocalDate.now())

fun createIdentifikator(lokalid: String, gyldighetsdato: LocalDate?): Identifikator =
    Identifikator(
        lokalid = Lokalid(lokalid),
        gyldighetsdato = gyldighetsdato ?: LocalDate.now()
    )

