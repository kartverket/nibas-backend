package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
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
    @Suppress("unused") //Er kun ubrukt i påvente av TS-1192
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository
) {

    fun hentAlleNasjoner(gyldighetsdato: LocalDate = LocalDate.now()): List<Nasjon> = nasjonRepository.findAllGyldigPaaDato(gyldighetsdato)
    fun hentNasjon(identifikator: Identifikator): Nasjon? = nasjonRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentGrenserForNasjon(identifikator: Identifikator): Set<GrenseKobling> =
        hentNasjon(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptySet()

    @Transactional(readOnly = false)
    fun oppdaterNasjon(
        lokalid: Lokalid,
        gyldigFra: LocalDate,
        endringstype: Endringstype,
        oppdatertNasjon: NasjonRequest
    ): Nasjon? {

        val identifikator = Identifikator(lokalid, gyldigFra)
        val gammelNasjon = hentNasjon(identifikator)
            ?: throw ResponseException.NotFoundException("Nasjon med $identifikator ble ikke funnet.")

        verifiserOptimistiskLaas(oppdatertNasjon.version, gammelNasjon)?.let {
            throw OptimistiskLaasException(it)
        }

        val nyNasjon = createNyVersjonAvNasjon(nasjon = gammelNasjon, gyldigFra = gyldigFra, endringstype = endringstype)
        return nyNasjon

        //Kommentert vekk det med adm.enhetnavn, siden klienten pr.nå ikke oppdaterer navn. Lot det ligge pga TS-1192
            // Finner basert på språkkode, og oppdaterer den
//            oppdatertNasjon.administrativenhetnavn.forEach { navn ->
//                gammel.nasjonLokalid.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
//                    verifiserOptimistiskLaas(navn.version, gammeltNavn)?.let { throw OptimistiskLaasException(it) }
//                    gammeltNavn.navn = navn.navn
//                    administrativenhetnavnRepository.save(gammeltNavn)
//                }
//            }
    }

    fun createNyVersjonAvNasjon(nasjon: Nasjon, gyldigFra: LocalDate, endringstype: Endringstype): Nasjon {
        val nyNasjon = nasjonRepository.save(
            Nasjon(lokalid = nasjon.lokalid)
                .apply {
                    applyMappingFromExistingNasjon(nasjon)
                    setGyldigFra(gyldigFra)
                    version = nesteVersjonForNasjon(Lokalid(nasjon.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )
        return nyNasjon
            .applyCascadingOperationsBasedOnExistingNasjon(nasjon)
            .also { nasjon.setGyldigTil(gyldigFra)  }
    }

    private fun nesteVersjonForNasjon(lokalId: Lokalid): Int =
        nasjonRepository
            .findAllByLokalid(lokalId.value)
            .maxBy { it.version }
            .version + 1

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int = nasjonRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)
}
