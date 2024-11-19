package no.kartverket.nibas.builders


import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.StemmekretsRequest

class StemmekretsRequestBuilder(
    private var stemmekretsnavn: String = "Mosekollen øst 2",
    private var lokalId: String = "00c5ecde-145c-4757-8bea-6290f0b513db",
    private var stemmekretsnummer: String = "138",
    private var tellekretsnummer: String? = "13",
    private var tellekretsnavn: String? = "Mosekollen minikrets 3",
    private var valgdistriktsnummer: String? = "89",
    private var kommunenummer: Kommunenummer = Kommunenummer("0301", "0301"),
    private var version: Int = 1
) {
    companion object {
        val DEFAULT_REQUEST = StemmekretsRequestBuilder().build()
    }

    fun withLokalId(lokalId: String): StemmekretsRequestBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withStemmekretsnummer(stemmekretsnummer: String): StemmekretsRequestBuilder {
        this.stemmekretsnummer = stemmekretsnummer
        return this
    }
    fun withNavn(navn: String): StemmekretsRequestBuilder {
        this.stemmekretsnavn = navn
        return this
    }

    fun withVersion(version: Int): StemmekretsRequestBuilder {
        this.version = version
        return this
    }

    fun build(): StemmekretsRequest = StemmekretsRequest(
        nummer = stemmekretsnummer,
        identifikasjon = Identifikasjon(
            lokalid = lokalId,
        ),
        navn = stemmekretsnavn,
        tellekretsnummer = tellekretsnummer,
        tellekretsnavn = tellekretsnavn,
        valgdistriktsnummer = valgdistriktsnummer,
        kommunenummer = kommunenummer,
        version = version
    )
}
