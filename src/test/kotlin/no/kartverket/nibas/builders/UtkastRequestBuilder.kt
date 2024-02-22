package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.domain.Endringstype
import java.time.LocalDate
import java.time.Month

class UtkastRequestBuilder {

    private var gyldigFra: LocalDate = LocalDate.of(2023, Month.JANUARY, 1)
    private var navn = "Utkastet mitt"
    private var endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
    private var operasjoner = Operasjoner(
        metadataendringer = Metadataendringer(
            kommuneendringer = mapOf("6n49a68e-bb52-49f4-b668-48cabb0b8585" to KommuneRequestBuilder.DEFAULT_REQUEST)
        )
    )
    private var version: Int = 1

    companion object {
        val DEFAULT_REQUEST = UtkastRequestBuilder().build()
    }

    fun build(): OppdaterUtkastRequest {
        return OppdaterUtkastRequest(
            navn = this.navn,
            endringstype = this.endringstype.value,
            gyldigFra = this.gyldigFra,
            operasjoner = this.operasjoner,
            version = version
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

    fun withEndringstype(endringstype: Endringstype): UtkastRequestBuilder {
        this.endringstype = endringstype
        return this
    }

    fun withOperasjoner(operasjoner: Operasjoner): UtkastRequestBuilder {
        this.operasjoner = operasjoner
        return this
    }

}

class OpprettUtkastRequestBuilder {
    private var navn = "Mitt nye utkast"
    private var endringstype = Endringstype.VEDTATT_GRENSEJUSTERING.value
    companion object {
        val DEFAULT_REQUEST = OpprettUtkastRequestBuilder().build()
    }

    fun build(): OpprettUtkastRequest {
        return OpprettUtkastRequest(
            navn = this.navn,
            endringstype = this.endringstype,
        )
    }

    fun withNavn(navn: String): OpprettUtkastRequestBuilder {
        this.navn = navn
        return this
    }
    fun withEndringstype(endringstype: String): OpprettUtkastRequestBuilder {
        this.endringstype= endringstype
        return this
    }

}
