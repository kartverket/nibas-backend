package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.ekstern.FylkeApi
import no.kartverket.nibas.api.v1.ekstern.extensions.flate.toEksternFylkeRef
import no.kartverket.nibas.api.v1.ekstern.extensions.flate.toEksternFylkeResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternFylkeRef
import no.kartverket.nibas.api.v1.ekstern.response.EksternFylkeResponse
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.service.FylkeService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("EksternFylkeController")
class FylkeController(
    private val fylkeService: FylkeService,
    private val hrefProvider: HrefProvider
) : FylkeApi {

    override fun hentFylke(lokalid: String, gyldighetsdato: LocalDate?): EksternFylkeResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return fylkeService.hentFylke(identifikator)?.toEksternFylkeResponse(identifikator)
            ?: throw NotFoundException("Fylke $identifikator ble ikke funnet.")
    }
    override fun hentFylker(gyldighetsdato: LocalDate?): List<EksternFylkeRef> {
        val datoToUse: LocalDate = gyldighetsdato ?: LocalDate.now()
        return fylkeService.hentAlleFylker(datoToUse).map {
            val identifikatorForFylke = Identifikator(Lokalid(it.lokalid), datoToUse)
            it.toEksternFylkeRef(identifikatorForFylke, fylkeService, hrefProvider)
        }
    }
}
