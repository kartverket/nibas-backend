package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.request.UtkastRequest
import no.kartverket.nibas.domain.utkast.UtkastEndringstype
import java.time.LocalDate
import java.time.Month

class UtkastRequestBuilder {

    private var navn = "Utkastet mitt"

    fun build(): UtkastRequest {
        val kommuneRequest = KommuneRequestBuilder().build()
        return UtkastRequest(
            navn = this.navn,
            endringstype = UtkastEndringstype.VEDTATT_GRENSEJUSTERING.value,
            gyldigFra = LocalDate.of(2023, Month.JANUARY, 1),
            operasjoner = Operasjoner(
                metadataendringer = Metadataendringer(
                    kommuneendringer = mapOf("UUID-1" to kommuneRequest))),
        )
    }

    fun withNavn(navn: String): UtkastRequestBuilder {
        this.navn = navn
        return this
    }

}
