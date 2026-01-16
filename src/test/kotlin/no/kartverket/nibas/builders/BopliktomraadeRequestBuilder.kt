package no.kartverket.nibas.builders


import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.BopliktomraadeRequest

class BopliktomraadeRequestBuilder(
    private var lokalId: String = "00c5ecde-145c",
    private var bopliktomraadenummer: String = "138",
    private var bopliktomraadenavn: String = "Mosekollen øst 2",
    private var delvisBoplikt: Boolean = true,
    private var forskriftsreferanse: String? = "Mosekollen minikrets 3",
    private var url: String? = "https://www.kartverket.no/",
    private var kommunenummer: Kommunenummer = Kommunenummer("0301", "0301"),
    private var version: Int = 1
) {
    companion object {
        val DEFAULT_REQUEST = BopliktomraadeRequestBuilder().build()
    }

    fun withLokalId(lokalId: String): BopliktomraadeRequestBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withBopliktomraadenummer(bopliktomraadenummer: String): BopliktomraadeRequestBuilder {
        this.bopliktomraadenummer = bopliktomraadenummer
        return this
    }
    fun withNavn(navn: String): BopliktomraadeRequestBuilder {
        this.bopliktomraadenavn = navn
        return this
    }

    fun withVersion(version: Int): BopliktomraadeRequestBuilder {
        this.version = version
        return this
    }

    fun build(): BopliktomraadeRequest = BopliktomraadeRequest(
        identifikasjon = Identifikasjon(
            lokalid = lokalId,
        ),
        nummer = bopliktomraadenummer,
        navn = bopliktomraadenavn,
        delvisBoplikt = delvisBoplikt,
        forskriftsreferanse = forskriftsreferanse,
        url = url,
        kommunenummer = kommunenummer,
        version = version
    )
}
