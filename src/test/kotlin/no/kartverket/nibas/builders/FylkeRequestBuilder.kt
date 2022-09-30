package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.FylkeRequest

class FylkeRequestBuilder() {
    companion object {
        val DEFAULT_REQUEST = FylkeRequestBuilder().build()
    }

    private var navn = "Vestland"
    private var spraak = "NOR"
    private var lokalId = "123"
    private var navnerom = "A.B.C"
    private var fylkesnummerUuid = "UUID-1"
    private var samiskforvaltningsomraade = false

    fun build(): FylkeRequest = FylkeRequest(
        administrativenhetnavn = listOf(AdministrativEnhetNavn(navn, spraak, version = 1)),
        lokalid = lokalId,
        navnerom = navnerom,
        fylkesnummerId = fylkesnummerUuid,
        samiskforvaltningsomraade = samiskforvaltningsomraade,
        version = 1
    )

    fun withNavn(navn: String): FylkeRequestBuilder {
        this.navn = navn
        return this
    }

    fun withNavnerom(navnerom: String): FylkeRequestBuilder {
        this.navnerom = navnerom
        return this
    }

    fun withLokalId(lokalId: String): FylkeRequestBuilder {
        this.lokalId = lokalId
        return this
    }
}
