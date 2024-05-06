package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.InndelingerApi
import no.kartverket.nibas.api.v1.klient.response.InndelingSearchResponse
import no.kartverket.nibas.service.InndelingerService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientInndelingerController")
class InndelingerController(
    val inndelingerService: InndelingerService
): InndelingerApi {
    override fun finnInndelinger(searchString: String, gyldighetsdato: LocalDate, limit: Int): List<InndelingSearchResponse> {
        return inndelingerService.finnInndelinger(searchString = searchString, gyldighetsdato = gyldighetsdato, limit = limit)
            .map { InndelingSearchResponse(
                    id = it.id,
                    type = it.type,
                    nummer = it.nummer,
                    navn = it.navn,
                    representasjonspunkt = it.representasjonspunkt
                )
            }
    }
}
