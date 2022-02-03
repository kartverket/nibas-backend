package no.kartverket.nibas.api.v1.request

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

data class NasjonRequest(
    @Schema(description = "Navnene til nasjonen")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "LokalId for angitt nasjon")
    @NotBlank
    val lokalid: String,

    @Schema(description = "Navnerommet for nasjonen")
    val navnerom: String,
)

data class FylkeRequest(
    @Schema(description = "Navnene til fylket")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "LokalId for angitt fylke")
    @NotBlank
    val lokalid: String,

    @Schema(description = "Navnerommet for fylket")
    val navnerom: String,

    @Schema(description = "UUID til fylkesnummeret")
    @NotBlank
    val fylkesnummerId: String,

    @Schema(description = "Angir om fylket er et samisk forvaltningsområde")
    @NotNull
    val samiskforvaltningsomraade: Boolean,
)

@Schema(description = "Representasjon av en kommune")
data class KommuneRequest(

    @Schema(description = "Navnene til kommunen")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "LokalId for angitt kommune")
    @NotBlank
    val lokalid: String,

    //TODO: Vurdere å gjøre påkrevet, fordi det er en påkrevet egenskap i sosi-modellen
    @Schema(description = "Navnerommet for kommunen")
    val navnerom: String?,

    @Schema(description = "UUID til kommunenummeret")
    @NotBlank
    val kommunenummerId: String,

    @Schema(description = "Angir om kommunen er et samisk forvaltningsområde")
    @NotNull
    val samiskforvaltningsomraade: Boolean,
)
