package no.kartverket.nibas.api.v1.common

import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.response.ObjektIdentifikator


data class UnikKretsDelingEndring(
    val opprinneligKrets: IdentifikatorMedVersjon,
    val flatetype: Flatetype,
    val kommuneId: ObjektIdentifikator,
    val kretsNavn: String,
    val kretsNummer: String
)
fun getUnikeKretsDelingEndringRequests(kretsDelingEndringRequests: List<KretsDelingEndringRequest>): List<UnikKretsDelingEndring> {
    return kretsDelingEndringRequests.flatMap { getUnikeKretsDelingEndringRequests(it) }
}

/**
KretsDelingEndringRequest inneholder lister med navn og nummere.
Denne funksjonen lager en liste med unike kretsDelingEndringRequests som kun representerer én deling
 */
fun getUnikeKretsDelingEndringRequests(kretsDelingRequest: KretsDelingEndringRequest): List<UnikKretsDelingEndring> {
    return kretsDelingRequest.nyeKretser.map {
        UnikKretsDelingEndring(
            opprinneligKrets = kretsDelingRequest.opprinneligKrets,
            flatetype = kretsDelingRequest.flatetype,
            kommuneId = kretsDelingRequest.kommuneId,
            kretsNavn = it.kretsNavn,
            kretsNummer = it.kretsNummer
        )
    }
}
