package no.kartverket.nibas.api.v1.common

import io.swagger.v3.oas.annotations.media.Schema

enum class AdministrativEnhetType {
    FYLKE, KOMMUNE, NASJON
}

enum class StatistiskEnhetType {
    GRUNNKRETS
}

// Vurderte å innføre en sealed class som hadde AdministrativEnhet og StatistiskEnhet (ala dette https://stackoverflow.com/a/56771284), men det gjør det litt mer kronglete når man skal gruppere på undertypene senere.
enum class KontekstEnhetType {
    FYLKE, KOMMUNE, NASJON, GRUNNKRETS
}

@Schema(description = "Representasjon av et fylkesnummer")
data class Fylkesnummer(

    @Schema(description = "Unik UUID for fylkesnummeret")
    val id: String,

    @Schema(description = "Det faktiske fylkesnummeret")
    val kodeverdi: Int
)

@Schema(description = "Representasjon av et navn")
data class AdministrativEnhetNavn(

    @Schema(description = "Navnet på en administrativ enhet")
    val navn: String,

    @Schema(description = "Språkkoden til det administrative navnet")
    val spraak: String
)
