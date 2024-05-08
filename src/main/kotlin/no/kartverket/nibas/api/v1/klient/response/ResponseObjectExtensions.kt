package no.kartverket.nibas.api.v1.klient.response

import no.kartverket.nibas.api.v1.common.Flatetype

fun StemmekretsResponse.toInndelingResponse(): InndelingResponse = InndelingResponse(
    id = id,
    type = Flatetype.STEMMEKRETS,
    navn = navn,
    nummer = nummer,
    representasjonspunkt = representasjonspunkt,
    gyldighet = gyldighet
)

fun GrunnkretsResponse.toInndelingResponse(): InndelingResponse = InndelingResponse(
    id = id,
    type = Flatetype.GRUNNKRETS,
    navn = navn,
    nummer = nummer,
    representasjonspunkt = representasjonspunkt,
    gyldighet = gyldighet
)
