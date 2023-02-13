package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.StemmekretsApi
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.grenseKoblingToFeature
import no.kartverket.nibas.service.toStemmekretsResponse
import no.kartverket.nibas.kodelister.KodelisteService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientStemmekretsController")
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
