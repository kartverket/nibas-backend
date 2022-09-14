package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.UtkastEndringstype
import no.kartverket.nibas.api.v1.request.UtkastRequest
import java.time.LocalDate
import java.time.Month

class UtkastRequestBuilder {

    private var navn = "Utkastet mitt"

    fun build(): UtkastRequest = UtkastRequest(
        navn = this.navn,
        endringstype = UtkastEndringstype.VEDTATT_GRENSEENDRING,
        gyldigFra = LocalDate.of(2023, Month.JANUARY, 1),
        kommuneendringer = listOf(KommuneRequestBuilder().build())
    )

    fun withNavn(navn: String): UtkastRequestBuilder {
        this.navn = navn
        return this
    }

}
