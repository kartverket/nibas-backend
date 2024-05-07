package no.kartverket.nibas.builders


import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.KommuneRequest

class KommuneRequestBuilder {
    companion object {
        val DEFAULT_REQUEST = KommuneRequestBuilder().build()
    }

    private var navn = "Oslo"
    private var spraak = "NOR"
    private var lokalId = "kommune123"
    private var samiskforvaltningsomraade = false
    private var version = 1

    fun build(): KommuneRequest = KommuneRequest(
        administrativenhetnavn = listOf(AdministrativEnhetNavn(navn, spraak, version = 1)),
        lokalid = lokalId,
        samiskforvaltningsomraade = samiskforvaltningsomraade,
        version = version
    )

    fun withNavn(navn: String): KommuneRequestBuilder {
        this.navn = navn
        return this
    }

    fun withLokalId(lokalId: String): KommuneRequestBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withVersion(version: Int): KommuneRequestBuilder {
        this.version = version
        return this
    }

    fun withSamiskforvaltningsomraade(samiskforvaltningsomraade: Boolean): KommuneRequestBuilder {
        this.samiskforvaltningsomraade = samiskforvaltningsomraade
        return this
    }

}
