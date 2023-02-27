package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.response.GrunnkretsRef
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.toGyldighetResponse
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
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

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId: Lokalid, dato: LocalDate): List<Grunnkrets> =
        grunnkretsRepository.findAllByLokalidSortedByGyldigFra(lokalId)
            .filter { it.gyldighet.gyldigFra.isAfter(dato) }

    /**
     * Id her må være lokalid.
     * Vet at grunnkretsRequestWrapper.gyldigFra er satt, og må være lik i dag eller fram i tid.
     */
    fun oppdaterGrunnkrets(
        lokalId: Lokalid,
        gyldigFra: LocalDate,
        endringstype: Endringstype,
        oppdatertGrunnkrets: GrunnkretsRequest
    ): Grunnkrets {
        val grunnkretsHelper =
            GrunnkretsHelper(grunnkretsRepository.findAllByLokalidSortedByGyldigFra(lokalId))

        // Må bruke grunnkrets som matcher lokalId på gyldigFra-tidspunkt
        val relevantGrunnkrets =
            grunnkretsHelper.findRelevantGrunnkretsForGyldigFra(gyldigFra, lokalId)
        verifiserOptimistiskLaas(oppdatertGrunnkrets.version, relevantGrunnkrets)?.let {
            throw OptimistiskLaasException(it)
        }

        // Må oppdatere forrige rad på tidslinja
        relevantGrunnkrets.setGyldigTil(gyldigFra)

        // Uansett om gyldigFra er identisk med grunnkretsens gyldigFra eller fram i tid: Lage ny
        // rad (ny uuid, samme lokalid).
        val nyGrunnkrets =
            grunnkretsRepository.save(
                Grunnkrets(lokalid = relevantGrunnkrets.lokalid)
                    // Mappe alle øvrige felter fra eksisterende grunnkrets
                    .applyMappingFromExistingGrunnkrets(relevantGrunnkrets)
                    .applyChangesFromRequest(oppdatertGrunnkrets)
            )

        return nyGrunnkrets
            .applyCascadingOperationsBasedOnExistingGrunnkrets(relevantGrunnkrets)
            .apply {
                setGyldigFra(gyldigFra)
                if (grunnkretsHelper.existsRowsWithLaterGyldigFraDato(relevantGrunnkrets)) {
                    // Setter gyldigTil lik gyldigFra for neste rad
                    setGyldigTil(grunnkretsHelper.getNesteGyldigFraDato(relevantGrunnkrets))
                }
                this.auditInfo.typeEndring = endringstype
                this.version = grunnkretsHelper.getNextVersion()
            }
    }

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

    fun getNextVersion() = grunnkretserSortedByGyldigFra.maxBy { it.version }.version + 1
}

// Setter feltene som faktisk kan endres i request
private fun Grunnkrets.applyChangesFromRequest(request: GrunnkretsRequest): Grunnkrets {
    return this.apply {
        grunnkretsnavn = request.navn
        grunnkretsnummer = request.grunnkretsnummer
    }
}

fun Grunnkrets.toGrunnkretsRef(identifikator: Identifikator, antallFramtidigeVersjoner: Int = 0, hrefProvider: HrefProvider): GrunnkretsRef =
    GrunnkretsRef(
        id = identifikator.toObjektIdentifikator(),
        navn = this.grunnkretsnavn!!,
        grunnkretsnummer = this.grunnkretsnummer!!,
        href = hrefProvider.getHref(identifikator),
        antallFramtidigeVersjoner = antallFramtidigeVersjoner
    )


fun Grunnkrets.toGrunnkretsResponse(identifikator: Identifikator): GrunnkretsResponse =
    GrunnkretsResponse(
        id = identifikator.toObjektIdentifikator(),
        navn = this.grunnkretsnavn!!,
        grunnkretsnummer = this.grunnkretsnummer!!,
        gyldighet = this.gyldighet.toGyldighetResponse(),
        oppdateringsdato = this.auditInfo.endretdato,
        endringstype = this.auditInfo.typeEndring.value,
        features = FeatureCollection(features = listOf(
            posisjonToFeature(this.posisjon, this.version),
            flateToFeature(this, this.version)
        )),
        version = this.version
    )


fun Grunnkrets.toKretsResponse(kodelisteService: KodelisteService): KretsResponse = KretsResponse(
    id = this.lokalid,
    navn = this.grunnkretsnavn!!,
    nummer = this.grunnkretsnummer!!,
    kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
    features = FeatureCollection(
        features = listOf(
            posisjonToFeature(this.posisjon, this.version),
            flateToFeature(this, this.version)
        )
    )
)
