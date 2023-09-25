package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.NasjonRequest

class NasjonRequestBuilder {

    companion object {
        val DEFAULT_NASJON = NasjonRequestBuilder().build()
    }

    private var navn: String = "Norge"
    private var spraak: String = "nor"
    private var lokalId: String = "nasjon123"
    private var version = 1

    fun build(): NasjonRequest = NasjonRequest(
        listOf(AdministrativEnhetNavn(navn, spraak, version = 1, rekkefoelge = null)),
        lokalid = lokalId,
        version = version)

    fun withLokalId(lokalId: String): NasjonRequestBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withNavn(navn: String): NasjonRequestBuilder {
        this.navn = navn
        return this
    }

    fun withVersion(version: Int): NasjonRequestBuilder {
        this.version = version
        return this
    }

}
