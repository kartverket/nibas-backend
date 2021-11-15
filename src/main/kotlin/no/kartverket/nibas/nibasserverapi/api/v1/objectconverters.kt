package no.kartverket.nibas.nibasserverapi.api.v1

import no.kartverket.nibas.nibasserverapi.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.nibasserverapi.domain.AdministrativEnhet
import no.kartverket.nibas.nibasserverapi.domain.Fylke
import no.kartverket.nibas.nibasserverapi.domain.Kommune
import no.kartverket.nibas.nibasserverapi.api.v1.response.AdministrativEnhet as AdministrativEnhetResponse
import no.kartverket.nibas.nibasserverapi.api.v1.request.AdministrativEnhet as AdministrativEnhetRequest

fun toAdministrativEnhetResponse(administrativEnhet: AdministrativEnhet, type: AdministrativEnhetType): AdministrativEnhetResponse = AdministrativEnhetResponse(
    type,
    administrativEnhet.id,
    administrativEnhet.navn,
    administrativEnhet.nummer
)

fun fromAdministrativEnhetRequest(id: Int, administrativEnhet: AdministrativEnhetRequest): AdministrativEnhet {
    return when(administrativEnhet.type) {
        AdministrativEnhetType.KOMMUNE -> Kommune (
            id,
            administrativEnhet.navn,
            administrativEnhet.nummer)
        AdministrativEnhetType.FYLKE -> Fylke (
            id,
            administrativEnhet.navn,
            administrativEnhet.nummer)
    }

}