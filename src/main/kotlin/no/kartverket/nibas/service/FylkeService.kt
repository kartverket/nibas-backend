package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
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
class FylkeService(
    private val fylkeRepository: FylkeRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository
) {

    fun hentAlleFylker(gyldighetsdato: LocalDate = LocalDate.now()): List<Fylke> = fylkeRepository.findAllGyldigPaaDato(gyldighetsdato)

    fun hentFylke(identifikator: Identifikator): Fylke? = fylkeRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentGrenserForFylke(identifikator: Identifikator): Set<GrenseKobling> =
        hentFylke(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptySet()

    @Transactional(readOnly = false)
    fun oppdaterFylke(lokalid: Lokalid, oppdatertFylke: FylkeRequest): Fylke? {
        return hentFylke(Identifikator(lokalid))?.let { gammel ->
            verifiserOptimistiskLaas(oppdatertFylke.version, gammel)?.let { throw OptimistiskLaasException(it) }
            gammel.samiskforvaltningsomraade = oppdatertFylke.samiskforvaltningsomraade
            gammel.fylkesnummer = oppdatertFylke.fylkesnummerId

            // Finner basert på språkkode, og oppdaterer den
            oppdatertFylke.administrativenhetnavn.forEach { navn ->
                gammel.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
                    verifiserOptimistiskLaas(navn.version, gammeltNavn)?.let { throw OptimistiskLaasException(it) }
                    gammeltNavn.navn = navn.navn
                    administrativenhetnavnRepository.save(gammeltNavn)
                }
            }
            fylkeRepository.save(gammel)
        }
    }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int = fylkeRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)
}
