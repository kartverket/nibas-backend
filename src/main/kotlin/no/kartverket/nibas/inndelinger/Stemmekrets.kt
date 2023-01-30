package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.StemmekretsApi
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsRef
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.api.v1.response.toGyldighetResponse
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class StemmekretsController(
    private val stemmekretsService: StemmekretsService,
    private val kodelisteService: KodelisteService,
) : StemmekretsApi {

    override fun hentStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): StemmekretsResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return stemmekretsService.hentStemmekrets(identifikator)?.toStemmekretsResponse(identifikator, kodelisteService)
            ?: throw NotFoundException("Stemmekrets $identifikator ble ikke funnet.")
    }

    override fun hentGrenserForStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grenser = stemmekretsService.hentGrenserForStemmekrets(identifikator)
        if (grenser.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grenser
                .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { grenseKoblingToFeature(it, KontekstEnhetType.STEMMEKRETS, identifikator.gyldighetsdato) }
        )
    }

    override fun hentFramtidigeVersjonerForStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): List<StemmekretsResponse> =
        stemmekretsService.hentFramtidigeVersjonerForStemmekrets(Lokalid(lokalid), gyldighetsdato)
            .map { it.toStemmekretsResponse(Identifikator(Lokalid(it.lokalid), it.gyldighet.gyldigFra), kodelisteService) }

}

@Transactional(readOnly = true)
@Service
class StemmekretsService(private val stemmekretsRepository: StemmekretsRepository) {

    fun hentStemmekrets(identifikator: Identifikator): Stemmekrets? = stemmekretsRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentSisteGyldigeStemmekrets(lokalid: String): Stemmekrets? =
        stemmekretsRepository.findAllByLokalid(lokalid = lokalid)
            .filter { !it.gyldighet.slettet }
            .maxBy { it.gyldighet.gyldigFra }

    fun hentGrenserForStemmekrets(identifikator: Identifikator): List<GrenseKobling> {
        return hentStemmekrets(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptyList()
    }

    @Transactional(readOnly = false)
    fun oppdaterStemmekrets(lokalId: Lokalid, gyldigFra: LocalDate, endringstype: Endringstype ,oppdatertStemmekrets: StemmekretsRequest): Stemmekrets? {
        return hentStemmekrets(Identifikator(lokalId, gyldigFra))?.let { gammelStemmekrets ->
            verifiserOptimistiskLaas(oppdatertStemmekrets.version, gammelStemmekrets)?.let { throw OptimistiskLaasException(it) }

            stemmekretsRepository.save(
                Stemmekrets(lokalid = gammelStemmekrets.lokalid).apply {
                    applyMappingFromExistingStemmekrets(gammelStemmekrets)
                    applyMappingFromStemmekretsRequest(oppdatertStemmekrets)
                    setGyldigFra(gyldigFra)
                    auditInfo.typeEndring = endringstype
                    version = nesteVersjonForStemmekrets(lokalId)
                }
            )
                .applyCascadingOperationsBasedOnExistingStemmekrets(gammelStemmekrets)
                .also {gammelStemmekrets.setGyldigTil(gyldigFra) }
        }
    }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int =
        stemmekretsRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)

    fun hentFramtidigeVersjonerForStemmekrets(lokalId: Lokalid, gyldighetsdato: LocalDate?): List<Stemmekrets> {
        val identifikator = Identifikator(lokalId, gyldighetsdato ?: LocalDate.now())
        val gyldigStemmekretsForDagensDato = hentStemmekrets(identifikator)
            ?: throw UgyldigTilstandException("Det finnes ingen gyldig stemmekrets for identifikator $identifikator")
        return listOf(gyldigStemmekretsForDagensDato)
            .plus(hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId, identifikator.gyldighetsdato))
    }

    private fun nesteVersjonForStemmekrets(lokalId: Lokalid): Int =
        stemmekretsRepository
            .findAllByLokalid(lokalId.value)
            .maxBy { it.version }
            .version + 1

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId: Lokalid, dato: LocalDate): List<Stemmekrets> =
        stemmekretsRepository.findAllByLokalidSortedByGyldigFra(lokalId)
            .filter { it.gyldighet.gyldigFra.isAfter(dato) }
}

fun Stemmekrets.applyMappingFromStemmekretsRequest(stemmekretsRequest: StemmekretsRequest): Stemmekrets =
    this.apply {
        stemmekretsRequest.stemmekretsnavn?.let { this.stemmekretsnavn = it }
        stemmekretsRequest.stemmekretsnummer?.let { this.stemmekretsnummer = it }
        stemmekretsRequest.tellekretsnummer?.let { this.tellekretsnummer = it }
        stemmekretsRequest.tellekretsnavn?.let { this.tellekretsnavn = it }
        stemmekretsRequest.valgdistriktsnummer?.let { this.valgdistriktsnummer = it }
    }

fun Stemmekrets.toStemmekretsRef(identifikator: Identifikator, antallFramtidigeVersjoner: Int = 0, hrefProvider: HrefProvider): StemmekretsRef {
    return StemmekretsRef(
        id = identifikator.toObjektIdentifikator(),
        navn = this.stemmekretsnavn!!,
        nummer = this.stemmekretsnummer!!,
        href = hrefProvider.getHref(identifikator),
        antallFramtidigeVersjoner = antallFramtidigeVersjoner
    )
}

fun Stemmekrets.toStemmekretsResponse(identifikator: Identifikator, kodelisteService: KodelisteService): StemmekretsResponse {
    return StemmekretsResponse(
        id = identifikator.toObjektIdentifikator(),
        stemmekretsnavn = this.stemmekretsnavn!!,
        stemmekretsnummer = this.stemmekretsnummer!!,
        kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
        valgdistriktsnummer = this.valgdistriktsnummer,
        tellekretsnavn = this.tellekretsnavn,
        gyldighet = this.gyldighet.toGyldighetResponse(),
        endringstype = this.auditInfo.typeEndring.value,
        oppdateringsdato = this.auditInfo.endretdato,
        tellekretsnummer = this.tellekretsnummer,
        features = FeatureCollection(features = listOf(
            posisjonToFeature(this.posisjon, this.version),
            flateToFeature(this, this.version))),
        version = this.version
    )
}

fun Stemmekrets.toKretsResponse(kodelisteService: KodelisteService): KretsResponse = KretsResponse(
    id = this.lokalid,
    navn = this.stemmekretsnavn!!,
    nummer = this.stemmekretsnummer!!,
    kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
    features = FeatureCollection(
        features = listOf(
            posisjonToFeature(this.posisjon, this.version),
            flateToFeature(this, this.version)
        )
    )
)

