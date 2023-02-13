package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.FylkeApi
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.FylkeRef
import no.kartverket.nibas.api.v1.response.FylkeResponse
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.service.FylkeService
import no.kartverket.nibas.service.grenseKoblingToFeature
import no.kartverket.nibas.service.toFylkeRef
import no.kartverket.nibas.service.toFylkeResponse
import no.kartverket.nibas.kodelister.KodelisteService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientFylkeController")
class FylkeController(
    private val fylkeService: FylkeService,
    private val kodelisteService: KodelisteService,
    private val hrefProvider: HrefProvider
) : FylkeApi {

    override fun hentFylker(gyldighetsdato: LocalDate?): List<FylkeRef> {
        val datoToUse: LocalDate = gyldighetsdato ?: LocalDate.now()
        return fylkeService.hentAlleFylker(datoToUse).map {
            val identifikatorForFylke = Identifikator(Lokalid(it.lokalid), datoToUse)
            it.toFylkeRef(identifikatorForFylke, fylkeService.hentAntallGyldigeVersjonerFramITidFor(identifikatorForFylke), hrefProvider)
        }
    }

    override fun hentFylke(lokalid: String, gyldighetsdato: LocalDate?): FylkeResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return fylkeService.hentFylke(identifikator)?.toFylkeResponse(identifikator, kodelisteService)
            ?: throw NotFoundException("Fylke $identifikator ble ikke funnet.")
    }

    override fun hentGrenserForFylke(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grenseKoblinger = fylkeService.hentGrenserForFylke(identifikator)
        if (grenseKoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grenseKoblinger.map {
                grenseKoblingToFeature(
                    grenseKobling = it,
                    gyldighetsdato = identifikator.gyldighetsdato)
            }
        )
    }

}
