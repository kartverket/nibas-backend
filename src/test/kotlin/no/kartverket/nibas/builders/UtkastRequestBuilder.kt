package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.request.UtkastRequest
import no.kartverket.nibas.domain.utkast.UtkastEndringstype
import java.time.LocalDate
import java.time.Month

class UtkastRequestBuilder {

    private var gyldigFra: LocalDate = LocalDate.of(2023, Month.JANUARY, 1)
    private var navn = "Utkastet mitt"
    private var endringstype = UtkastEndringstype.VEDTATT_GRENSEJUSTERING
    private var operasjoner = Operasjoner(
        metadataendringer = Metadataendringer(
            kommuneendringer = mapOf("6n49a68e-bb52-49f4-b668-48cabb0b8585" to KommuneRequestBuilder.DEFAULT_REQUEST)
        )
    )

    companion object {
        val DEFAULT_REQUEST = UtkastRequestBuilder().build()
    }

    fun build(): UtkastRequest {
        return UtkastRequest(
            navn = this.navn,
            endringstype = this.endringstype.value,
            gyldigFra = this.gyldigFra,
            operasjoner = this.operasjoner,
        )
    }

    fun withNavn(navn: String): UtkastRequestBuilder {
        this.navn = navn
        return this
    }

    fun withGyldigFra(gyldigFra: LocalDate): UtkastRequestBuilder {
        this.gyldigFra = gyldigFra
        return this
    }

}
