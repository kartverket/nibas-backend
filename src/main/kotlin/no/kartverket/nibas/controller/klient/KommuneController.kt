package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.KommuneApi
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.GrunnkretsRef
import no.kartverket.nibas.api.v1.response.KommuneRef
import no.kartverket.nibas.api.v1.response.KommuneResponse
import no.kartverket.nibas.api.v1.response.StemmekretsRef
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.extensions.flate.toKommuneResponse
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientKommuneController")
class KommuneController(
    private val kommuneService: KommuneService,
    private val kodelisteService: KodelisteService,
    private val grunnkretsService: GrunnkretsService,
    private val stemmekretsService: StemmekretsService,
    private val hrefProvider: HrefProvider
) : KommuneApi {

    override fun hentKommuner(fylkeId: String?, gyldighetsdato: LocalDate?): List<KommuneRef> {
        val datoToUse: LocalDate = gyldighetsdato ?: LocalDate.now()
        return if (fylkeId == null) {
            kommuneService.hentAlleKommuner(datoToUse).map {
                kommuneService.kommuneToKommuneRef(it, datoToUse)
            }
        } else {
            kommuneService.hentKommunerForFylke(Identifikator(Lokalid(fylkeId), datoToUse)).map {
                kommuneService.kommuneToKommuneRef(it, datoToUse)
            }
        }
    }

    override fun hentKommune(lokalid: String, gyldighetsdato: LocalDate?): KommuneResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return kommuneService.hentKommune(identifikator)?.let {
            it.toKommuneResponse(identifikator, kodelisteService.createKommunenummerFromUUID(it.kommunenummer))
        } ?: throw NotFoundException("Kommune $identifikator ble ikke funnet.")
    }

    override fun hentGrenserForKommune(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grenseKoblinger = kommuneService.hentGrenserForKommune(identifikator)
        if (grenseKoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grenseKoblinger.map { it.grenseKoblingToFeature(gyldighetsdato = identifikator.gyldighetsdato) }
        )
    }

    override fun hentKommunesGrunnkretser(lokalid: String, gyldighetsdato: LocalDate?): List<GrunnkretsRef> {
        val kommuneIdentifikator = createIdentifikator(lokalid, gyldighetsdato)
        return kommuneService.hentKommunesGrunnkretser(kommuneIdentifikator).map {
            val identifikatorForGrunnkrets = Identifikator(lokalid = Lokalid(it.lokalid), gyldighetsdato = kommuneIdentifikator.gyldighetsdato)
            it.toGrunnkretsRef(identifikatorForGrunnkrets, grunnkretsService.hentAntallGyldigeVersjonerFramITidFor(identifikatorForGrunnkrets), hrefProvider, kodelisteService)
        }
    }

    override fun hentKommunesStemmekretser(lokalid: String, gyldighetsdato: LocalDate?): List<StemmekretsRef> {
        val kommuneIdentifikator = createIdentifikator(lokalid, gyldighetsdato)
        return kommuneService.hentKommunesStemmekretser(kommuneIdentifikator).map {
            val identifikatorForStemmekrets = Identifikator(lokalid = Lokalid(it.lokalid), gyldighetsdato = kommuneIdentifikator.gyldighetsdato)
            it.toStemmekretsRef(identifikatorForStemmekrets, stemmekretsService.hentAntallGyldigeVersjonerFramITidFor(identifikatorForStemmekrets), hrefProvider, kodelisteService)
        }
    }
}

private fun Grunnkrets.toGrunnkretsRef(identifikator: Identifikator, antallFramtidigeVersjoner: Int = 0, hrefProvider: HrefProvider, kodelisteService: KodelisteService): GrunnkretsRef = GrunnkretsRef(
    id = identifikator.toObjektIdentifikator(),
    navn = this.grunnkretsnavn!!,
    grunnkretsnummer = this.grunnkretsnummer!!,
    href = hrefProvider.getHref(identifikator),
    kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
    antallFramtidigeVersjoner = antallFramtidigeVersjoner
)

fun Stemmekrets.toStemmekretsRef(identifikator: Identifikator, antallFramtidigeVersjoner: Int = 0, hrefProvider: HrefProvider, kodelisteService: KodelisteService): StemmekretsRef = StemmekretsRef(
    id = identifikator.toObjektIdentifikator(),
    navn = this.stemmekretsnavn!!,
    nummer = this.stemmekretsnummer!!,
    href = hrefProvider.getHref(identifikator),
    kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
    antallFramtidigeVersjoner = antallFramtidigeVersjoner,
    version = this.version,
)
