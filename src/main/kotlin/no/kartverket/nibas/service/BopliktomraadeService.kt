package no.kartverket.nibas.service

import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.repository.BopliktomraadeRepository
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

}

