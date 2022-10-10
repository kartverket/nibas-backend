package no.kartverket.nibas.builders


import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest

class GrunnkretsRequestBuilder(
    private var navn: String = "Mosekollen øst",
    private var lokalId: String = "00c5ecde-145c-4757-8bea-6290f0b513db",
    private var navnerom: String = "A.B.C",
    private var grunnkretsnummer: String = "03015212",
    private var version: Int = 1
) {
    companion object {
        val DEFAULT_REQUEST = GrunnkretsRequestBuilder().build()
    }

    fun build(): GrunnkretsRequest = GrunnkretsRequest(
        navn = navn,
        grunnkretsnummer = grunnkretsnummer,
        identifikasjon = Identifikasjon(
            lokalid = lokalId,
            navnerom = navnerom,
        ),
        version = version
    )
}
