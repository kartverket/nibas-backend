package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.NasjonApi
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.NasjonResponse
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.service.NasjonService
import no.kartverket.nibas.service.grenseKoblingToFeature
import no.kartverket.nibas.service.toNasjonRef
import no.kartverket.nibas.service.toNasjonResponse
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import no.kartverket.nibas.api.v1.response.NasjonRef as NasjonRefResponse

@RestController
class NasjonController(
    private val nasjonService: NasjonService,
    private val hrefProvider: HrefProvider
) : NasjonApi {

    override fun hentNasjoner(gyldighetsdato: LocalDate?): List<NasjonRefResponse> {
        val datoToUse: LocalDate = gyldighetsdato?: LocalDate.now()
        return nasjonService.hentAlleNasjoner(datoToUse).map {
            val identifikatorForNasjon = Identifikator(Lokalid(it.lokalid), datoToUse)
            it.toNasjonRef(identifikatorForNasjon, nasjonService.hentAntallGyldigeVersjonerFramITidFor(identifikatorForNasjon), hrefProvider)
        }
    }

    override fun hentNasjon(lokalid: String, gyldighetsdato: LocalDate?): NasjonResponse {
        val identifikatorForNasjon = createIdentifikator(lokalid, gyldighetsdato)
        return nasjonService.hentNasjon(identifikatorForNasjon)?.toNasjonResponse(identifikatorForNasjon)
            ?: throw NotFoundException("Nasjon $identifikatorForNasjon ble ikke funnet.")
    }

    override fun hentGrenserForNasjon(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grensekoblinger = nasjonService.hentGrenserForNasjon(identifikator)
        if (grensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grensekoblinger.map {
                grenseKoblingToFeature(
                    grenseKobling = it,
                    gyldighetsdato = identifikator.gyldighetsdato)
            }
        )
    }
}
