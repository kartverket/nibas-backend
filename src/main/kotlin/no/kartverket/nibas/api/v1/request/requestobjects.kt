package no.kartverket.nibas.api.v1.request

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import javax.validation.constraints.NotBlank

@Schema(description = "Representasjon av en administrativ enhet")
class AdministrativEnhet(

    @Schema(description = "Type administrativ enhet")
    @NotBlank
    val type: AdministrativEnhetType,

    @Schema(description = "Navnet til den administrative enheten")
    val navn: List<AdministrativEnhetNavn>,

    // TODO: Dette vil være ID for fylke, kommune etc. Vurdere å rename felt.
    @Schema(description = "Nummeret som identifiserer den administrative enheten")
    @NotBlank
    val nummer: String
)

@Schema(description = "Representasjon av et navn")
class AdministrativEnhetNavn(

    @Schema(description = "Navnet på en administrativ enhet")
    val navn: String,

    @Schema(description = "Språkkoden til det administrative navnet")
    val spraak: String
)
