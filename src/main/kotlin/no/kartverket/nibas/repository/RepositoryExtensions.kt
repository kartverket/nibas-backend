package no.kartverket.nibas.repository

import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.domain.ContainsSosiIdentifikasjon
import no.kartverket.nibas.sporing.Gyldig
import java.time.LocalDate

const val REPOSITORY_EXTENSIONS_FILEPATH = "no.kartverket.nibas.repository.RepositoryExtensionsKt"

/**
 * Finner alle rader gyldige på gitt gyldighetsdato.
 */
fun <T> CrudRepositoryWithLokalIdFinder<T, String>.findAllGyldigPaaDato(gyldighetsdato: LocalDate = LocalDate.now()): List<T> where T : Gyldig =
    findAll()
        .filter { it.gyldigPaaDato(gyldighetsdato) }
        .sortedBy { it.gyldighet.gyldigFra }

/**
 * Slår opp det gyldige objektet for gitt identifikator.
 * Hvis ingen treff returneres null.
 * Hvis flere treff kastes exception (programmeringsfeil).
 */
fun <T> CrudRepositoryWithLokalIdFinder<T, String>.findGyldigObjektForIdentifikator(identifikator: Identifikator): T?
    where T : Gyldig, T : ContainsSosiIdentifikasjon =
    findGyldigObjektPaaGyldighetsdato(findAllByLokalid(identifikator.lokalid.value), identifikator.gyldighetsdato)

fun <T> CrudRepositoryWithLokalIdFinder<T, String>.findAllGyldigeObjekterForIdentifikatorer(identifikatorer: List<Identifikator>): Map<Identifikator, T>
    where T : Gyldig, T : ContainsSosiIdentifikasjon {
    var map: Map<Identifikator, T> = emptyMap()
    identifikatorer.forEach {
        val gyldigObjekt = findGyldigObjektForIdentifikator(it)
        if (gyldigObjekt != null) {
            map = map.plus(Pair(it, gyldigObjekt))
        }
    }
    return map
}


/**
 * Finner alle ikke-slettede objekter som matcher lokalid.
 */
fun <T> CrudRepositoryWithLokalIdFinder<T, String>.findAllByLokalidSortedByGyldigFra(lokalid: Lokalid): List<T>
    where T : Gyldig, T : ContainsSosiIdentifikasjon =
    findAllByLokalid(lokalid.value)
        .filter { !it.gyldighet.slettet }
        .sortedBy { it.gyldighet.gyldigFra }

/**
 * Finne alle rader som matcher:
 * -identifikator
 * -har et gyldighetsintervall som er gyldig framover i tid (inkluderer ikke nåværende gyldig rad)
 * Sorteres på gyldigFra-dato
 */
fun <T> CrudRepositoryWithLokalIdFinder<T, String>.findAllFramtidigeVersjonerByIdentifikator(identifikator: Identifikator): List<T>
    where T : Gyldig, T : ContainsSosiIdentifikasjon {
    val versjonerAvObjekt = findAllByLokalidSortedByGyldigFra(identifikator.lokalid)
    val versjonGyldigPaaTidspunkt = findGyldigObjektPaaGyldighetsdato(versjonerAvObjekt, identifikator.gyldighetsdato)
    val indexOfCurrentRad = versjonerAvObjekt.indexOf(versjonGyldigPaaTidspunkt)
    return if (indexOfCurrentRad == versjonerAvObjekt.lastIndex) { // Ingen framtidige versjoner
        emptyList()
    } else {
        versjonerAvObjekt.subList(indexOfCurrentRad + 1, versjonerAvObjekt.size) // Returnerer alle etter current rad
    }
}


fun <T> CrudRepositoryWithLokalIdFinder<T, String>.findAntallFramtidigeVersjonerForIdentifikator(identifikator: Identifikator): Int
    where T : Gyldig, T : ContainsSosiIdentifikasjon = findAllFramtidigeVersjonerByIdentifikator(identifikator).size

/**
 * Finner det gyldige objektet blant gitt liste av gyldigObjekter for gitt gyldighetsdato.
 * Hvis ingen treff returneres null.
 * Hvis flere treff kastes exception (programmeringsfeil).
 */
private fun <T> findGyldigObjektPaaGyldighetsdato(gyldigObjekter: List<T>, gyldighetsdato: LocalDate = LocalDate.now()): T?
    where T : Gyldig, T : ContainsSosiIdentifikasjon {
    val matchendeObjekter = gyldigObjekter.filter { it.gyldigPaaDato(gyldighetsdato) }
    if (matchendeObjekter.isEmpty()) {
        return null
    } else if (matchendeObjekter.size > 1) { // Bør ikke kunne skje, da er det programmeringsfeil
        val firstMatchendeObjekt = matchendeObjekter.first()
        val typeBeskrivelse = "av type ${firstMatchendeObjekt::class.java.simpleName}"
        throw UgyldigTilstandException("Fant ${matchendeObjekter.size} objekter $typeBeskrivelse for lokalid '${firstMatchendeObjekt.lokalid}' gyldig på dato $gyldighetsdato")
    }
    return matchendeObjekter.first()
}
