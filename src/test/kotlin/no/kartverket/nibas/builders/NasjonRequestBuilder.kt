package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.NasjonRequest

class NasjonRequestBuilder {

    companion object {
        val DEFAULT_NASJON = NasjonRequestBuilder().build()
    }

    private var navn: String = "Norge"
    private var spraak: String = "nor"
    private var lokalId: String = "123"

    fun build(): NasjonRequest = NasjonRequest(
        listOf(AdministrativEnhetNavn(navn, spraak, version = 1)),
        lokalid = lokalId,
        version = 1)

    fun withLokalId(lokalId: String): NasjonRequestBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withNavn(navn: String): NasjonRequestBuilder {
        this.navn = navn
        return this
    }

}
