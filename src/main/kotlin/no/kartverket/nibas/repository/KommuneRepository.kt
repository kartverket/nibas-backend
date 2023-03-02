package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Kommune
import java.time.LocalDate

const val KOMMUNEREPOSITORY_EXTENSIONS_FILEPATH = "no.kartverket.nibas.repository.KommuneRepositoryKt"

interface KommuneRepository : CrudRepositoryWithLokalIdFinder<Kommune, String> {
    fun findByKommunenummer(kommunenummer: String): Kommune?
    fun findAllByKommunenummer(kommunenummer: String): List<Kommune>
}

fun KommuneRepository.findGyldigKommuneForKommunenummer(kommunenummer: String, gyldighetsdato: LocalDate): Kommune? {
    return findGyldigObjektPaaGyldighetsdato(findAllByKommunenummer(kommunenummer), gyldighetsdato)
}

