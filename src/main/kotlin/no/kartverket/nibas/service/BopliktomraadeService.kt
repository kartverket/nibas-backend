package no.kartverket.nibas.service

import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.BopliktomraadeGrenseKobling
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.repository.BopliktomraadeRepository
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
@Service
@Transactional
class BopliktomraadeService(
    private val bopliktRepository: BopliktomraadeRepository
) {
    @Transactional
    fun hentAlleBopliktomraader(dato: LocalDate): List<Bopliktomraade> =
        bopliktRepository.findAll()
            .filter { it.gyldigPaaDato(dato) }

    @Transactional(readOnly = true)
    fun hentBopliktomraade(identifikator: Identifikator): Bopliktomraade? =
        bopliktRepository.findGyldigObjektForIdentifikator(identifikator)

    @Transactional(readOnly = true)
    fun hentGrensekoblingerForBopliktomraade(identifikator: Identifikator): List<BopliktomraadeGrenseKobling> {
        return hentBopliktomraade(identifikator)
            ?.getGrensekoblinger(identifikator.gyldighetsdato)?.toList()
            ?: emptyList()
    }

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId: Lokalid, dato: LocalDate): List<Bopliktomraade> =
        bopliktRepository.findAllByLokalidSortedByGyldigFra(lokalId)
            .filter { it.gyldighet.gyldigFra.isAfter(dato) }
}


