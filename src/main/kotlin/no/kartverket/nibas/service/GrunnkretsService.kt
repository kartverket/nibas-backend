package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findAllFramtidigeVersjonerByIdentifikator
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional
@Service
class GrunnkretsService(private val grunnkretsRepository: GrunnkretsRepository) {

    @Transactional(readOnly = true)
    fun hentGrunnkrets(identifikator: Identifikator): Grunnkrets? = grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)

    @Transactional(readOnly = true)
    fun hentSisteGyldigeGrunnkrets(lokalid: String): Grunnkrets? =
        grunnkretsRepository.findAllByLokalid(lokalid = lokalid)
            .filter { !it.gyldighet.slettet }
            .maxBy { it.gyldighet.gyldigFra }


    @Transactional(readOnly = true)
    fun hentGrenserForGrunnkrets(identifikator: Identifikator): List<GrenseKobling> {
        return hentGrunnkrets(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptyList()
    }

    @Transactional(readOnly = true)
    fun hentGrunnkretserForIds(ids: Set<String>, gyldighetsdato: LocalDate): List<Grunnkrets> {
        return ids
            .map { createIdentifikator(it, gyldighetsdato) }
            .map { hentGrunnkrets(it) ?: throw NotFoundException("Grunnkrets med $it ble ikke funnet.") }
            .sortedBy { it.id }
    }

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId: Lokalid, dato: LocalDate): List<Grunnkrets> =
        grunnkretsRepository.findAllByLokalidSortedByGyldigFra(lokalId)
            .filter { it.gyldighet.gyldigFra.isAfter(dato) }

    fun oppdaterGrunnkrets(
        lokalId: Lokalid,
        gyldigFra: LocalDate,
        endringstype: Endringstype,
        oppdatertGrunnkrets: GrunnkretsRequest
    ): Grunnkrets {
        val identifikator = Identifikator(lokalId, gyldigFra)
        val gammelGrunnkrets = hentGrunnkrets(identifikator)
            ?: throw NotFoundException("Grunnkrets med $identifikator ble ikke funnet.")

        verifiserOptimistiskLaas(oppdatertGrunnkrets.version, gammelGrunnkrets)?.let {
            throw OptimistiskLaasException(it)
        }

        //Lar dette med GrunnkretsHelper ligge, det har noe å gjøre med evt flere fremtidige endringer på grunnkrets
        val grunnkretsHelper = GrunnkretsHelper(grunnkretsRepository.findAllByLokalidSortedByGyldigFra(lokalId))

        // Må bruke grunnkrets som matcher lokalId på gyldigFra-tidspunkt
        val relevantGrunnkrets = grunnkretsHelper.findRelevantGrunnkretsForGyldigFra(gyldigFra, lokalId)

        // Uansett om gyldigFra er identisk med grunnkretsens gyldigFra eller fram i tid: Lage ny
        // rad (ny uuid, samme lokalid).
        val nyGrunnkrets = createNyVersjonAvGrunnkrets(gammelGrunnkrets, gyldigFra, endringstype)
            .applyChangesFromRequest(oppdatertGrunnkrets)

        return nyGrunnkrets
            .apply {
                setGyldigFra(gyldigFra)
                if (grunnkretsHelper.existsRowsWithLaterGyldigFraDato(relevantGrunnkrets)) {
                    // Setter gyldigTil lik gyldigFra for neste rad
                    setGyldigTil(grunnkretsHelper.getNesteGyldigFraDato(relevantGrunnkrets))
                }
            }
    }

    fun createNyVersjonAvGrunnkrets(grunnkrets: Grunnkrets, gyldigFra: LocalDate, endringstype: Endringstype): Grunnkrets {
        return grunnkretsRepository.save(
            Grunnkrets(lokalid = grunnkrets.lokalid)
                .apply {
                    applyMappingFromExistingGrunnkrets(grunnkrets)
                    setGyldigFra(gyldigFra)
                    version = nesteVersjonForGrunnkrets(Lokalid(grunnkrets.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )
            .applyCascadingOperationsBasedOnExistingGrunnkrets(grunnkrets)
            .also { grunnkrets.setGyldigTil(gyldigFra)  }
    }

    private fun nesteVersjonForGrunnkrets(lokalId: Lokalid): Int =
        grunnkretsRepository
            .findAllByLokalid(lokalId.value)
            .maxBy { it.version }
            .version + 1

    /**
     * Returnerer liste med grunnkretser som matcher lokalid og gyldighetsdato.
     * Nåværende gyldig rad og fremtidig publiserte gyldige rader.
     *
     * -Første innslag er raden som er gyldig på gyldighetsdato, default dagens dato.
     * -Så følger de framtidige gyldige publiserte versjonene sortert på gyldigFra.
     */
    fun hentFramtidigeVersjonerForGrunnkrets(identifikator: Identifikator): List<Grunnkrets> {
        val gyldigGrunnkretsForDagensDato = grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)
            ?: throw UgyldigTilstandException("Det finnes ingen gyldig grunnkrets for identifikator $identifikator")
        return listOf(gyldigGrunnkretsForDagensDato)
            .plus(grunnkretsRepository.findAllFramtidigeVersjonerByIdentifikator(identifikator))
    }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int = grunnkretsRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)

}

// Setter feltene som faktisk kan endres i request
private fun Grunnkrets.applyChangesFromRequest(request: GrunnkretsRequest): Grunnkrets {
    return this.apply {
        grunnkretsnavn = request.navn
        grunnkretsnummer = request.grunnkretsnummer
    }
}

private class GrunnkretsHelper(private val grunnkretserSortedByGyldigFra: List<Grunnkrets>) {

    fun findRelevantGrunnkretsForGyldigFra(utkastetsGyldigFra: LocalDate, id: Lokalid): Grunnkrets =
        grunnkretserSortedByGyldigFra.find { grunnkrets -> grunnkrets.gyldigPaaDato(utkastetsGyldigFra) }
            ?: throw NotFoundException("Fant ikke eksisterende grunnkrets for lokalid $id og gyldigFra $utkastetsGyldigFra.")

    fun existsRowsWithLaterGyldigFraDato(grunnkrets: Grunnkrets): Boolean {
        return findIndex(grunnkrets) < grunnkretserSortedByGyldigFra.lastIndex
    }

    fun getNesteGyldigFraDato(grunnkrets: Grunnkrets): LocalDate {
        return grunnkretserSortedByGyldigFra[findIndex(grunnkrets) + 1].gyldighet.gyldigFra
    }

    private fun findIndex(grunnkrets: Grunnkrets) =
        grunnkretserSortedByGyldigFra.indices.find { grunnkretserSortedByGyldigFra[it].id == grunnkrets.id }
            ?: throw RuntimeException("Did not find index for grunnkrets $grunnkrets")
}
