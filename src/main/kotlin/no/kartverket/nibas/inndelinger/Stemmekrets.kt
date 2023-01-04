package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.StemmekretsApi
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsRef
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.StemmekretsRepository
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

}

@Transactional(readOnly = true)
@Service
class StemmekretsService(private val stemmekretsRepository: StemmekretsRepository) {

    fun hentStemmekrets(identifikator: Identifikator): Stemmekrets? = stemmekretsRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentGrenserForStemmekrets(identifikator: Identifikator): List<GrenseKobling> {
        return hentStemmekrets(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptyList()
    }

    @Transactional(readOnly = false)
    fun oppdaterStemmekrets(identifikator: Identifikator, oppdatertStemmekrets: StemmekretsRequest): Stemmekrets? {
        return hentStemmekrets(identifikator)?.let { gammel: Stemmekrets ->
            verifiserOptimistiskLaas(oppdatertStemmekrets.version, gammel)?.let { throw OptimistiskLaasException(it) }
            gammel.stemmekretsnavn = oppdatertStemmekrets.stemmekretsnavn
            gammel.stemmekretsnummer = oppdatertStemmekrets.stemmekretsnummer
            gammel.tellekretsnavn = oppdatertStemmekrets.tellekretsnavn
            gammel.tellekretsnummer = oppdatertStemmekrets.tellekretsnummer
            gammel.valgdistriktsnummer = oppdatertStemmekrets.valgdistriktsnummer

            stemmekretsRepository.save(gammel)
        }
    }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int = stemmekretsRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)

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

