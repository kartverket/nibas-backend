package no.kartverket.nibas.repository

import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.domain.Grunnkrets
import java.time.LocalDate

const val REPOSITORY_EXTENSIONS_FILEPATH = "no.kartverket.nibas.repository.RepositoryExtensionsKt"

fun GrunnkretsRepository.findAllByLokalidSortedByGyldigFra(lokalid: Lokalid): List<Grunnkrets> =
    findAllByLokalid(lokalid.value)
        .filter { !it.gyldighet.slettet }
        .sortedBy { it.gyldighet.gyldigFra }

fun GrunnkretsRepository.findGyldigGrunnkretsForTidspunkt(lokalid: Lokalid, gyldigTidspunkt: LocalDate = LocalDate.now()): Grunnkrets =
    findGrunnkretsGyldigPaaDato(findAllByLokalidSortedByGyldigFra(lokalid), gyldigTidspunkt)

/**
 * Finne alle rader som matcher:
 * -lokalid
 * -har et gyldighetsintervall som er gyldig på framover i tid (inkluderer ikke nåværende gyldig rad)
 * Sorteres på gyldigFra-dato
 */
fun GrunnkretsRepository.findAllByLokalidFramITid(lokalid: Lokalid, gyldigTidspunkt: LocalDate = LocalDate.now()): List<Grunnkrets> {
    val grunnkretser = findAllByLokalidSortedByGyldigFra(lokalid)
    val grunnkretsGyldigPaaTidspunkt = findGrunnkretsGyldigPaaDato(grunnkretser, gyldigTidspunkt)
    val indexOfCurrentRad = grunnkretser.indexOf(grunnkretsGyldigPaaTidspunkt)
    return if (indexOfCurrentRad == grunnkretser.lastIndex) {
        emptyList()
    } else {
        grunnkretser.subList(indexOfCurrentRad + 1, grunnkretser.size)
    }
}

private fun findGrunnkretsGyldigPaaDato(grunnkretser: List<Grunnkrets>, gyldigTidspunkt: LocalDate = LocalDate.now()): Grunnkrets {
    val matchingGrunnkretser = grunnkretser.filter { it.gyldigPaaTidspunkt(gyldigTidspunkt) }
    if (matchingGrunnkretser.isEmpty()) {
        throw ResponseException.NotFoundException("Fant ingen grunnkrets gyldig på tidspunkt $gyldigTidspunkt")
    } else if (matchingGrunnkretser.size > 1) { // Bør ikke kunne skje, da er det programmeringsfeil
        throw UgyldigTilstandException("Fant ${matchingGrunnkretser.size} grunnkretser gyldig på tidspunkt $gyldigTidspunkt")
    }
    return matchingGrunnkretser.first()
}
