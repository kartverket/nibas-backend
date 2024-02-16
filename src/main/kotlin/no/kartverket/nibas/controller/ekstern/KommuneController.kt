package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.ekstern.KommuneApi
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.KommuneRef
import no.kartverket.nibas.api.v1.response.KommuneResponse
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.extensions.flate.toGrunnkretsResponse
import no.kartverket.nibas.extensions.flate.toKommuneRef
import no.kartverket.nibas.extensions.flate.toKommuneResponse
import no.kartverket.nibas.extensions.flate.toKretsResponse
import no.kartverket.nibas.extensions.flate.toStemmekretsResponse
import no.kartverket.nibas.service.KommuneService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("EksternKommuneController")
class KommuneController(
    private val kommuneService: KommuneService,
    private val hrefProvider: HrefProvider
) : KommuneApi {

    override fun hentKommuner(fylkeId: String?, gyldighetsdato: LocalDate?): List<KommuneRef> {
        val datoToUse: LocalDate = gyldighetsdato ?: LocalDate.now()
        return if (fylkeId == null) {
            val alleKommuner = kommuneService.hentAlleKommuner(datoToUse)
            val map = alleKommuner.map {
                it.toKommuneRef(datoToUse, kommuneService, hrefProvider)
            }
            map
        } else {
            kommuneService.hentKommunerForFylke(Identifikator(Lokalid(fylkeId), datoToUse)).map {
                it.toKommuneRef(datoToUse, kommuneService, hrefProvider)
            }
        }
    }
    override fun hentKommune(lokalid: String, gyldighetsdato: LocalDate?): KommuneResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return kommuneService.hentKommune(identifikator)?.let {
            it.toKommuneResponse(identifikator, Kommunenummer(it.kommunenummerUuid, it.kommunenummer))
        } ?: throw NotFoundException("Kommune $identifikator ble ikke funnet.")
    }

    override fun hentAlleStemmekretserForKommune(kommunenummer: String, gyldighetsdato: LocalDate?): List<KretsResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        return kommuneService.hentAlleStemmekretserForKommune(kommunenummer, dato).map { it.toKretsResponse() }
    }

    override fun hentAlleGrunnkretserForKommune(kommunenummer: String, gyldighetsdato: LocalDate?): List<KretsResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        return kommuneService.hentAlleGrunnkretserForKommune(kommunenummer, dato).map { it.toKretsResponse() }
    }

    override fun hentAlleStemmekretserMedDetaljerForKommune(kommunenummer: String, gyldighetsdato: LocalDate?): List<StemmekretsResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        return kommuneService.hentAlleStemmekretserForKommune(kommunenummer, dato).map {
            it.toStemmekretsResponse(Identifikator(Lokalid(it.lokalid), dato))
        }
    }

    override fun hentAlleGrunnkretserMedDetaljerForKommune(kommunenummer: String, gyldighetsdato: LocalDate?): List<GrunnkretsResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        return kommuneService.hentAlleGrunnkretserForKommune(kommunenummer, dato).map {
            it.toGrunnkretsResponse(Identifikator(Lokalid(it.lokalid), dato))
        }
    }
}
