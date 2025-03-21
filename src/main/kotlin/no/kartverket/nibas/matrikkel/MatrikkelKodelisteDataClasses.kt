package no.kartverket.nibas.matrikkel

import io.swagger.v3.oas.annotations.media.Schema

// API-typer
data class MatrikkelKodelisterRespons(
    @Schema(description = "Liste av målemetodekoder fra Matrikkelen.")
    val maalemetodeKodeliste: List<KodelisteEntryDTO> = listOf(),
    @Schema(description = "Liste av hjelpelinjetyper fra Matrikkelen.")
    val hjelpelinjetypeKodeliste: List<KodelisteEntryDTO> = listOf()
)

data class KodelisteEntryDTO(
    @Schema(description = "Id til koden.")
    val id: Long,
    @Schema(description = "Kodeverdi for koden, kan være SOSI-kodeverdi.")
    val kodeverdi: String,
    @Schema(description = "Beskrivende navn til koden på bokmål.")
    val navn: String
)
