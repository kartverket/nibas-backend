package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.FylkeRequest

class FylkeRequestBuilder() {
    companion object {
        val DEFAULT_REQUEST = FylkeRequestBuilder().build()
    }

    private var navn = "Vestland"
    private var spraak = "NOR"
    private var lokalId = "fylke123"
    private var samiskforvaltningsomraade = false
    private var version = 1

    fun build(): FylkeRequest = FylkeRequest(
        administrativenhetnavn = listOf(AdministrativEnhetNavn(navn, spraak, version = 1)),
        lokalid = lokalId,
        samiskforvaltningsomraade = samiskforvaltningsomraade,
        version = version
    )

    fun withNavn(navn: String): FylkeRequestBuilder {
        this.navn = navn
        return this
    }

    fun withLokalId(lokalId: String): FylkeRequestBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withVersion(version: Int): FylkeRequestBuilder {
        this.version = version
        return this
    }

    fun withSamiskforvaltningsomraade(samiskforvaltningsomraade: Boolean): FylkeRequestBuilder {
        this.samiskforvaltningsomraade = samiskforvaltningsomraade
        return this
    }

}
