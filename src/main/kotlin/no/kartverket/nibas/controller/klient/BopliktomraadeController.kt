package no.kartverket.nibas.controller.klient
import no.kartverket.nibas.api.v1.klient.BopliktomraadeApi
import no.kartverket.nibas.api.v1.klient.response.BopliktomraadeResponse
import no.kartverket.nibas.extensions.flate.toBopliktomraadeResponse
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
            bopliktomraade.toBopliktomraadeResponse(dato)
        }
    }

}
