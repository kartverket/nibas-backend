package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.klient.FylkeApi
import no.kartverket.nibas.api.v1.klient.response.FylkeResponse
import no.kartverket.nibas.api.v1.klient.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.klient.response.StemmekretsResponse
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.extensions.flate.toFylkeResponse
import no.kartverket.nibas.extensions.flate.toGrunnkretsResponse
import no.kartverket.nibas.extensions.flate.toStemmekretsResponse
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import no.kartverket.nibas.service.FylkeService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientFylkeController")
class FylkeController(
    private val fylkeService: FylkeService,
) : FylkeApi {

    override fun hentFylker(gyldighetsdato: LocalDate?): List<FylkeResponse> {
        val datoToUse: LocalDate = gyldighetsdato ?: LocalDate.now()
        return fylkeService.hentAlleFylker(datoToUse).map { it.toFylkeResponse() }
    }

    override fun hentFylke(lokalid: String, gyldighetsdato: LocalDate?): FylkeResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return fylkeService.hentFylke(identifikator)?.toFylkeResponse()
            ?: throw NotFoundException("Fylke $identifikator ble ikke funnet.")
    }

    override fun hentGrenserForFylke(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grenseKoblinger = fylkeService.hentGrensekoblingerForFylke(identifikator)
        if (grenseKoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grenseKoblinger.map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) }
        )
    }

    override fun hentFylkesGrunnkretser(lokalid: String, gyldighetsdato: LocalDate?): List<GrunnkretsResponse> {
        val identifikator = createIdentifikator(lokalid = lokalid, gyldighetsdato = gyldighetsdato)
        return fylkeService.hentAlleGrunnkretserIFylke(identifikator)
            .map { it.toGrunnkretsResponse() }
    }

    override fun hentFylkesStemmekretser(lokalid: String, gyldighetsdato: LocalDate?): List<StemmekretsResponse> {
        val identifikator = createIdentifikator(lokalid = lokalid, gyldighetsdato = gyldighetsdato)
        return fylkeService.hentAlleStemmekretserIFylke(identifikator)
            .map { it.toStemmekretsResponse() }    }


}
