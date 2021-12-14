package no.kartverket.nibas.api.v1.response

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetType

@Schema(description = "Representasjon av en administrativ enhet")
class AdministrativEnhet(
    val type: AdministrativEnhetType,

    @Schema(description = "Unik id for den administrative enheten")
    val id: Int,

    @Schema(description = "Navnet på den administrative enheten")
    val navn: List<AdministrativEnhetNavn>,
)

@Schema(description = "Representasjon av et navn")
class AdministrativEnhetNavn(

    @Schema(description = "Navnet på en administrativ enhet")
    val navn: String,

    @Schema(description = "Språkkoden til det administrative navnet")
    val spraak: String
)