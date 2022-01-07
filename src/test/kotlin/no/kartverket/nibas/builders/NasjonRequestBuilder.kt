package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.NasjonRequest as NasjonRequest

class NasjonRequestBuilder(private val id: Int = 1) {

    companion object {
        val DEFAULT_NASJON = NasjonRequestBuilder().build()
    }

    private var navn: String = "Norge"
    private var spraak: String = "nor"
    private var lokalId: String = "123"
    private var navnerom: String = "ABC"

    fun build(): NasjonRequest = NasjonRequest(
        listOf(AdministrativEnhetNavn(navn, spraak)),
        lokalId,
        navnerom)

    fun withNavnerom(navnerom: String): NasjonRequestBuilder {
        this.navnerom = navnerom
        return this
    }

    fun withLokalId(lokalId: String): NasjonRequestBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withNavn(navn: String): NasjonRequestBuilder {
        this.navn = navn
        return this
    }

}