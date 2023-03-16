package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.GrunnkretsApi
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.api.v1.response.toGyldighetResponse
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.extensions.geometry.flateToFeature
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import no.kartverket.nibas.extensions.geometry.posisjonToFeature
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientGrunnkretsController")
class GrunnkretsController(
    private val grunnkretsService: GrunnkretsService,
    private val kodelisteService: KodelisteService
) : GrunnkretsApi {

    override fun hentGrunnkrets(lokalid: String, gyldighetsdato: LocalDate?): GrunnkretsResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return grunnkretsService.hentGrunnkrets(identifikator)?.let {
            it.toGrunnkretsResponse(identifikator, kodelisteService.createKommunenummerFromUUID(it.kommune.kommunenummer))
        } ?: throw NotFoundException("Grunnkrets $identifikator ble ikke funnet.")
    }

    override fun hentGrenserForGrunnkrets(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grensekoblinger = grunnkretsService.hentGrenserForGrunnkrets(identifikator)
        if (grensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grensekoblinger
                .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { it.grenseKoblingToFeature(KontekstEnhetType.GRUNNKRETS, identifikator.gyldighetsdato) }
        )
    }

    override fun hentFramtidigeVersjonerForGrunnkrets(lokalid: String, gyldighetsdato: LocalDate?): List<GrunnkretsResponse> {
        val identifikatorForGrunnkrets = createIdentifikator(lokalid, gyldighetsdato)
        return grunnkretsService.hentFramtidigeVersjonerForGrunnkrets(identifikatorForGrunnkrets)
            .map {
                it.toGrunnkretsResponse(
                    Identifikator(
                        lokalid = Lokalid(it.lokalid),
                        gyldighetsdato = it.gyldighet.gyldigFra),
                    kodelisteService.createKommunenummerFromUUID(it.kommune.kommunenummer)
                )
            }
    }
}

private fun Grunnkrets.toGrunnkretsResponse(identifikator: Identifikator, kommunenummer: Kommunenummer): GrunnkretsResponse =
    GrunnkretsResponse(
        id = identifikator.toObjektIdentifikator(),
        navn = this.grunnkretsnavn!!,
        grunnkretsnummer = this.grunnkretsnummer!!,
        gyldighet = this.gyldighet.toGyldighetResponse(),
        oppdateringsdato = this.auditInfo.endretdato,
        kommunenummer = kommunenummer,
        endringstype = this.auditInfo.typeEndring.value,
        features = FeatureCollection(features = listOf(
            this.posisjon.posisjonToFeature(this.version),
            flateToFeature(this.version)
        )),
        version = this.version
    )
