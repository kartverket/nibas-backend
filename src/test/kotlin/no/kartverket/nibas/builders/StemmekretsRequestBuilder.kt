package no.kartverket.nibas.builders


import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.StemmekretsRequest

class StemmekretsRequestBuilder {
    companion object {
        val DEFAULT_REQUEST = StemmekretsRequestBuilder().build()
    }

    private var stemmekretsnavn: String = "Mosekollen øst 2"
    private var lokalId = "00c5ecde-145c-4757-8bea-6290f0b513db"
    private var navnerom = "A.B.C"
    private var stemmekretsnummer = "138"
    private var tellekretsnummer: String = "13"
    private var tellekretsnavn: String = "Mosekollen minikrets 3"
    private var valgdistriktsnummer: String = "89"
    private var kommunenummer: String = "0301"

    fun build(): StemmekretsRequest = StemmekretsRequest(
        stemmekretsnummer = stemmekretsnummer,
        identifikasjon = Identifikasjon(
            lokalid = lokalId,
            navnerom = navnerom,
        ),
        stemmekretsnavn = stemmekretsnavn,
        tellekretsnummer = tellekretsnummer,
        tellekretsnavn = tellekretsnavn,
        valgdistriktsnummer = valgdistriktsnummer,
        kommunenummer = kommunenummer,
        version = 1
    )

    fun withStemmekretsnavn(navn: String): StemmekretsRequestBuilder {
        this.stemmekretsnavn = navn
        return this
    }

    fun withNavnerom(navnerom: String): StemmekretsRequestBuilder {
        this.navnerom = navnerom
        return this
    }

    fun withLokalId(lokalId: String): StemmekretsRequestBuilder {
        this.lokalId = lokalId
        return this
    }
}
