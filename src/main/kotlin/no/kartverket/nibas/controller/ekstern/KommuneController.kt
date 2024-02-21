package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.ekstern.KommuneApi
import no.kartverket.nibas.api.v1.ekstern.extensions.flate.toEksternGrunnkretsResponse
import no.kartverket.nibas.api.v1.ekstern.extensions.flate.toEksternKommuneRef
import no.kartverket.nibas.api.v1.ekstern.extensions.flate.toEksternKommuneResponse
import no.kartverket.nibas.api.v1.ekstern.extensions.flate.toEksternKretsResponse
import no.kartverket.nibas.api.v1.ekstern.extensions.flate.toEksternStemmekretsResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternGrunnkretsResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternKommuneRef
import no.kartverket.nibas.api.v1.ekstern.response.EksternKommuneResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternKretsResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternStemmekretsResponse
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.service.KommuneService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("EksternKommuneController")
class KommuneController(
    private val kommuneService: KommuneService,
    private val hrefProvider: HrefProvider
) : KommuneApi {

    override fun hentKommuner(fylkeId: String?, gyldighetsdato: LocalDate?): List<EksternKommuneRef> {
        val datoToUse: LocalDate = gyldighetsdato ?: LocalDate.now()
        return if (fylkeId == null) {
            kommuneService.hentAlleKommuner(datoToUse).map {
                it.toEksternKommuneRef(datoToUse, kommuneService, hrefProvider)
            }
        } else {
            kommuneService.hentKommunerForFylke(Identifikator(Lokalid(fylkeId), datoToUse)).map {
                it.toEksternKommuneRef(datoToUse, kommuneService, hrefProvider)
            }
        }
    }
    override fun hentKommune(lokalid: String, gyldighetsdato: LocalDate?): EksternKommuneResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return kommuneService.hentKommune(identifikator)?.let {
            it.toEksternKommuneResponse(identifikator, Kommunenummer(it.kommunenummerUuid, it.kommunenummer))
        } ?: throw NotFoundException("Kommune $identifikator ble ikke funnet.")
    }

    override fun hentAlleStemmekretserForKommune(kommunenummer: String, gyldighetsdato: LocalDate?): List<EksternKretsResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        return kommuneService.hentAlleStemmekretserForKommune(kommunenummer, dato).map { it.toEksternKretsResponse() }
    }

    override fun hentAlleGrunnkretserForKommune(kommunenummer: String, gyldighetsdato: LocalDate?): List<EksternKretsResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        return kommuneService.hentAlleGrunnkretserForKommune(kommunenummer, dato).map { it.toEksternKretsResponse() }
    }

    override fun hentAlleStemmekretserMedDetaljerForKommune(kommunenummer: String, gyldighetsdato: LocalDate?): List<EksternStemmekretsResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        return kommuneService.hentAlleStemmekretserForKommune(kommunenummer, dato).map {
            it.toEksternStemmekretsResponse(Identifikator(Lokalid(it.lokalid), dato))
        }
    }

    override fun hentAlleGrunnkretserMedDetaljerForKommune(kommunenummer: String, gyldighetsdato: LocalDate?): List<EksternGrunnkretsResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        return kommuneService.hentAlleGrunnkretserForKommune(kommunenummer, dato).map {
            it.toEksternGrunnkretsResponse(Identifikator(Lokalid(it.lokalid), dato))
        }
    }
}
