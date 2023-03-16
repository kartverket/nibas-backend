package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.findAllGyldigPaaDato
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class NasjonService(
    private val nasjonRepository: NasjonRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository
) {

    fun hentAlleNasjoner(gyldighetsdato: LocalDate = LocalDate.now()): List<Nasjon> = nasjonRepository.findAllGyldigPaaDato(gyldighetsdato)
    fun hentNasjon(identifikator: Identifikator): Nasjon? = nasjonRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentGrenserForNasjon(identifikator: Identifikator): Set<GrenseKobling> =
        hentNasjon(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptySet()

    @Transactional(readOnly = false)
    fun oppdaterNasjon(lokalid: Lokalid, oppdatertNasjon: NasjonRequest): Nasjon? {
        return hentNasjon(Identifikator(lokalid))?.let { gammel ->
            verifiserOptimistiskLaas(oppdatertNasjon.version, gammel)?.let { throw OptimistiskLaasException(it) }

            // Finner basert på språkkode, og oppdaterer den
            oppdatertNasjon.administrativenhetnavn.forEach { navn ->
                gammel.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
                    verifiserOptimistiskLaas(navn.version, gammeltNavn)?.let { throw OptimistiskLaasException(it) }
                    gammeltNavn.navn = navn.navn
                    administrativenhetnavnRepository.save(gammeltNavn)
                }
            }
            nasjonRepository.save(gammel)
        }
    }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int = nasjonRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)
}
