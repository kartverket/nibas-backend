package no.kartverket.nibas.api.v1.klient.response

import no.kartverket.nibas.api.v1.common.Flatetype

fun StemmekretsResponse.toInndelingResponse(): InndelingResponse = InndelingResponse(
    id,
    Flatetype.STEMMEKRETS,
    navn,
    nummer,
    representasjonspunkt
)

fun GrunnkretsResponse.toInndelingResponse(): InndelingResponse = InndelingResponse(
    id,
    Flatetype.GRUNNKRETS,
    navn,
    nummer,
    representasjonspunkt
)
