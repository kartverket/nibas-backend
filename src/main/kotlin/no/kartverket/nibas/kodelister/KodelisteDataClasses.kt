package no.kartverket.nibas.kodelister

import io.swagger.v3.oas.annotations.media.Schema

// API-typer
data class KodelisteRespons(
    @Schema(description = "Angir typen av kodeliste.")
    val type: KodelisteType,
    @Schema(description = "Liste av kodeliste-elementer.")
    val items: List<KodelisteItem> = listOf()
)

data class KodelisteItem(
    @Schema(description = "Id til kodeliste-innslaget.")
    val id: String,
    @Schema(description = "Beskrivelse av kodeliste-innslaget.")
    val label: String
)

enum class KodelisteType {
    @Schema(description = "Kodeliste for gyldige kommunenumre.")
    KOMMUNENUMMER,

    @Schema(description = "Kodeliste for gyldige fylkesnumre.")
    FYLKESNUMMER,

    @Schema(description = "Kodeliste for målemetode-koder.")
    MAALEMETODE_KODE,

}

// Intern type som holder på JSON-struktur slått opp i Geonorge
data class CachetKodelisteItem(
    // Angir typen av eksternt forvaltet kodeliste.
    val type: KodelisteType,
    // Inneholder kodelisteinnslaget fra Geonorge.
    val item: GeonorgeKodelisteItem
)

// Wrapper rundt respons-objektet ved kall til Geonorge-tjenesten
data class GeonorgeKodelisteResponse(
    val status: String, val containeditems: List<GeonorgeKodelisteItem>
)

// Respons-objektet ved kall til Geonorge-tjenesten
data class GeonorgeKodelisteItem(
    // Id er en URL til kodelisteinnslaget hos Geonorge.
    val id: String,
    // Kompakt tekst for framvisning.
    val label: String,
    // Angir språket, no for norsk.
    val lang: String,
    // Unik id.
    val uuid: String,
    // Beskriver om kodelisteinnslaget er gyldig.
    val status: String,
    // Beskrivelse av innslaget.
    val description: String,
    // Numerisk kode.
    val codevalue: String
)


