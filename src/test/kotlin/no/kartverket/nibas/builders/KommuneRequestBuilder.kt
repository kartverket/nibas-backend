package no.kartverket.nibas.builders


import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.KommuneRequest

class KommuneRequestBuilder {
    companion object {
        val DEFAULT_REQUEST = KommuneRequestBuilder().build()
    }

    private var navn = "Oslo"
    private var spraak = "NOR"
    private var lokalId = "123"
    private var navnerom = "A.B.C"
    private var kommunenummer = "123"
    private var samiskforvaltningsomraade = false

    fun build(): KommuneRequest = KommuneRequest(
        administrativenhetnavn = listOf(AdministrativEnhetNavn(navn, spraak)),
        lokalid = lokalId,
        navnerom = navnerom,
        kommunenummerId = kommunenummer,
        samiskforvaltningsomraade = samiskforvaltningsomraade
    )

    fun withNavn(navn: String): KommuneRequestBuilder {
        this.navn = navn
        return this
    }

    fun withNavnerom(navnerom: String): KommuneRequestBuilder {
        this.navnerom = navnerom
        return this
    }

    fun withLokalId(lokalId: String): KommuneRequestBuilder {
        this.lokalId = lokalId
        return this
    }
}
