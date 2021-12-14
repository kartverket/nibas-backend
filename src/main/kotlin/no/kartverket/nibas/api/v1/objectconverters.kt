package no.kartverket.nibas.api.v1

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.response.AdministrativEnhetNavn
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.api.v1.response.AdministrativEnhet as AdministrativEnhetResponse

fun toAdministrativEnhetResponse(administrativEnhet: AdministrativEnhet, type: AdministrativEnhetType): AdministrativEnhetResponse = AdministrativEnhetResponse(
    type,
    administrativEnhet.id!!,
    administrativEnhet.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!) }
)
