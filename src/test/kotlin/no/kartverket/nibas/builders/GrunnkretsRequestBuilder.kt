package no.kartverket.nibas.builders


import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest

class GrunnkretsRequestBuilder {
    companion object {
        val DEFAULT_REQUEST = GrunnkretsRequestBuilder().build()
    }

    private var navn = "Mosekollen øst"
    private var lokalId = "00c5ecde-145c-4757-8bea-6290f0b513db"
    private var navnerom = "A.B.C"
    private var grunnkretsnummer = "03015212"

    fun build(): GrunnkretsRequest = GrunnkretsRequest(
        navn = navn,
        grunnkretsnummer = grunnkretsnummer,
        identifikasjon = Identifikasjon(
            lokalid = lokalId,
            navnerom = navnerom,
        ),
        version = 1
    )

    fun withNavn(navn: String): GrunnkretsRequestBuilder {
        this.navn = navn
        return this
    }

    fun withNavnerom(navnerom: String): GrunnkretsRequestBuilder {
        this.navnerom = navnerom
        return this
    }

    fun withLokalId(lokalId: String): GrunnkretsRequestBuilder {
        this.lokalId = lokalId
        return this
    }
}
