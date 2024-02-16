package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.StemmekretsApi
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.klient.response.StemmekretsResponse
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.extensions.flate.toStemmekretsResponse
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientStemmekretsController")
class StemmekretsController(
    private val stemmekretsService: StemmekretsService
) : StemmekretsApi {

    override fun hentStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): StemmekretsResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return stemmekretsService.hentStemmekrets(identifikator)?.toStemmekretsResponse(identifikator)
            ?: throw NotFoundException("Stemmekrets $identifikator ble ikke funnet.")
    }

    override fun hentGrenserForStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grensekoblinger = stemmekretsService.hentGrensekoblingerForStemmekrets(identifikator)
        if (grensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grensekoblinger
                .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) }
        )
    }

    override fun hentFramtidigeVersjonerForStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): List<StemmekretsResponse> =
        stemmekretsService.hentFramtidigeVersjonerForStemmekrets(Lokalid(lokalid), gyldighetsdato)
            .map { it.toStemmekretsResponse(Identifikator(Lokalid(it.lokalid), it.gyldighet.gyldigFra)) }

}
