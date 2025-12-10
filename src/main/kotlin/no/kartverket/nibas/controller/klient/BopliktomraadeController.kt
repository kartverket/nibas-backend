package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.klient.BopliktomraadeApi
import no.kartverket.nibas.api.v1.klient.response.BopliktomraadeResponse
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.extensions.flate.toBopliktomraadeResponse
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import no.kartverket.nibas.service.BopliktomraadeService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientBopliktomraadeController")
class BopliktomraadeController(
    private val bopliktomraadeService: BopliktomraadeService
) : BopliktomraadeApi {
    override fun hentAlleBopliktomraader(gyldighetsdato: LocalDate?): List<BopliktomraadeResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        val alleBopliktomraader = bopliktomraadeService.hentAlleBopliktomraader(dato)

        return alleBopliktomraader.map { bopliktomraade ->
            bopliktomraade.toBopliktomraadeResponse()
        }
    }

    override fun hentBopliktomraade(lokalid: String, gyldighetsdato: LocalDate?): BopliktomraadeResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return bopliktomraadeService.hentBopliktomraade(identifikator)?.toBopliktomraadeResponse()
            ?: throw NotFoundException("Bopliktomraade $identifikator ble ikke funnet.")
    }

    override fun hentGrenserForBopliktomraade(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grensekoblinger = bopliktomraadeService.hentGrensekoblingerForBopliktomraade(identifikator)
        if (grensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grensekoblinger
                .map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) }
        )
    }
}

