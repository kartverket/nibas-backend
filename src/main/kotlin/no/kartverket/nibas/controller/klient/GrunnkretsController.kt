package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.GrunnkretsApi
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.extensions.flate.toGrunnkretsResponse
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientGrunnkretsController")
class GrunnkretsController(
    private val grunnkretsService: GrunnkretsService,
    private val kodelisteService: KodelisteService
) : GrunnkretsApi {

    override fun hentGrunnkrets(lokalid: String, gyldighetsdato: LocalDate?): GrunnkretsResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return grunnkretsService.hentGrunnkrets(identifikator)?.toGrunnkretsResponse(identifikator)
            ?: throw NotFoundException("Grunnkrets $identifikator ble ikke funnet.")
    }

    override fun hentGrenserForGrunnkrets(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grensekoblinger = grunnkretsService.hentGrensekoblingerForGrunnkrets(identifikator)
        if (grensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grensekoblinger
                .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) }
        )
    }

    override fun hentFramtidigeVersjonerForGrunnkrets(lokalid: String, gyldighetsdato: LocalDate?): List<GrunnkretsResponse> {
        val identifikatorForGrunnkrets = createIdentifikator(lokalid, gyldighetsdato)
        return grunnkretsService.hentFramtidigeVersjonerForGrunnkrets(identifikatorForGrunnkrets)
            .map {
                it.toGrunnkretsResponse(
                    Identifikator(
                        lokalid = Lokalid(it.lokalid),
                        gyldighetsdato = it.gyldighet.gyldigFra)
                )
            }
    }
}
