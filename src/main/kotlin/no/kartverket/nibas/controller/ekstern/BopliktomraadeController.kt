package no.kartverket.nibas.controller.ekstern
import no.kartverket.nibas.api.v1.ekstern.BopliktomraadeApi
import no.kartverket.nibas.api.v1.ekstern.extensions.flate.toEksternBopliktomraadeResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternBopliktomraadeRespons
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.service.BopliktomraadeService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
@RestController("EksternBopliktomraadeController")
class BopliktomraadeController(
    private val bopliktomraadeService: BopliktomraadeService
) : BopliktomraadeApi {

    override fun hentAlleBopliktomraader(gyldighetsdato: LocalDate?): List<EksternBopliktomraadeRespons> {
        val dato = gyldighetsdato ?: LocalDate.now()
        val alleBopliktomraader = bopliktomraadeService.hentAlleBopliktomraader(dato)

        return alleBopliktomraader.map { bopliktomraade ->
            bopliktomraade.toEksternBopliktomraadeResponse(dato)
        }
    }

    override fun hentBopliktomraade(lokalid: String, gyldighetsdato: LocalDate?): EksternBopliktomraadeRespons {
        val dato = gyldighetsdato ?: LocalDate.now()
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val bopliktomraade = bopliktomraadeService.hentBopliktomraade(identifikator)
            ?: throw NotFoundException("Fant ikke bopliktområde $lokalid")

        return bopliktomraade.toEksternBopliktomraadeResponse(dato)
    }
}



