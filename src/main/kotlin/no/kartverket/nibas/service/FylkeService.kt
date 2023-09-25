package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
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
    @Suppress("unused") //Er kun ubrukt i påvente av TS-1192
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository
) {

    fun hentAlleFylker(gyldighetsdato: LocalDate = LocalDate.now()): List<Fylke> = fylkeRepository.findAllGyldigPaaDato(gyldighetsdato)

    fun hentFylke(identifikator: Identifikator): Fylke? = fylkeRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentGrenserForFylke(identifikator: Identifikator): Set<GrenseKobling> =
        hentFylke(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptySet()

    @Transactional(readOnly = false)
    fun oppdaterFylke(
        lokalid: Lokalid,
        gyldigFra: LocalDate,
        endringstype: Endringstype,
        oppdatertFylke: FylkeRequest
    ): Fylke? {

        val identifikator = Identifikator(lokalid, gyldigFra)
        val gammeltFylke = hentFylke(Identifikator(lokalid))
            ?: throw ResponseException.NotFoundException("Fylke med $identifikator ble ikke funnet.")

        verifiserOptimistiskLaas(oppdatertFylke.version, gammeltFylke)?.let {
            throw OptimistiskLaasException(it)
        }

        val nyttFylke = createNyVersjonAvFylke(fylke = gammeltFylke, gyldigFra = gyldigFra, endringstype = endringstype)
            .applyChangesFromRequest(oppdatertFylke)

        return nyttFylke

            //Kommentert vekk det med adm.enhetnavn, siden klienten pr.nå ikke oppdaterer navn. Lot det ligge pga TS-1192
            // Finner basert på språkkode, og oppdaterer den
//            oppdatertFylke.administrativenhetnavn.forEach { navn ->
//                gammel.fylkeLokalid.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
//                    verifiserOptimistiskLaas(navn.version, gammeltNavn)?.let { throw OptimistiskLaasException(it) }
//                    gammeltNavn.navn = navn.navn
//                    administrativenhetnavnRepository.save(gammeltNavn)
//                }
//            }
    }

    fun createNyVersjonAvFylke(fylke: Fylke, gyldigFra: LocalDate, endringstype: Endringstype): Fylke {
        val nyFylke = fylkeRepository.save(
            Fylke(lokalid = fylke.lokalid)
                .apply {
                    applyMappingFromExistingFylke(fylke)
                    setGyldigFra(gyldigFra)
                    version = nesteVersjonForFylke(Lokalid(fylke.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )
        return nyFylke
            .applyCascadingOperationsBasedOnExistingFylke(fylke)
            .also { fylke.setGyldigTil(gyldigFra)  }
    }

    private fun nesteVersjonForFylke(lokalId: Lokalid): Int =
        fylkeRepository
            .findAllByLokalid(lokalId.value)
            .maxBy { it.version }
            .version + 1

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int = fylkeRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)
}

fun Fylke.applyChangesFromRequest(fylkeRequest: FylkeRequest): Fylke =
    this.apply {
        fylkeRequest.samiskforvaltningsomraade.let { this.samiskforvaltningsomraade = it }
    }
