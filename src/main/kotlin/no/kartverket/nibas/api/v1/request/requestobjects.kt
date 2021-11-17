package no.kartverket.nibas.api.v1.request

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Schema(description = "Representasjon av en administrativ enhet")
class AdministrativEnhet(

    @Schema(description = "Type administrativ enhet")
    @NotBlank
    val type: AdministrativEnhetType,

    @Schema(description = "Navnet til den administrative enheten")
    @NotBlank
    @Size(min = 0)
    val navn: String,

    @Schema(description = "Nummeret som identifiserer den administrative enheten")
    @NotBlank
    val nummer: String
)