package no.kartverket.nibas.api.v1.response

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Fylkesnummer
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.inndelinger.HistorikkStatus
import java.time.ZonedDateTime
import java.util.*

@Schema(description = "En referanse til en nasjon")
data class NasjonRef(

    @Schema(description = "ID-en til nasjonen")
    val id: String,

    @Schema(description = "Liste over navn til nasjonen")
    val navn: List<AdministrativEnhetNavn>,

    @Schema(description = "URL til full representasjon av nasjonen")
    val href: String
)

@Schema(description = "En referanse til et fylke")
data class FylkeRef(

    @Schema(description = "ID-en til fylket")
    val id: String,

    @Schema(description = "Liste over navn til fylket")
    val navn: List<AdministrativEnhetNavn>,

    @Schema(description = "URL til full representasjon av fylket")
    val href: String
)

@Schema(description = "En referanse til en kommune")
data class KommuneRef(

    @Schema(description = "ID-en til kommunen")
    val id: String,

    @Schema(description = "Liste over navn til kommunen")
    val navn: List<AdministrativEnhetNavn>,

    @Schema(description = "URL til full representasjon av kommunen")
    val href: String
)

@Schema(description = "En referanse til en historisk versjon av en administrativ enhet")
data class HistorikkRef(

    @Schema(description = "IDen til den historiske revisjonen")
    val revisionId: Int,

    @Schema(description = "Når revisjonen ble opprettet")
    val revisionDate: Date,

    @Schema(description = "Angir status for hva som medførte en ny revisjon")
    val historikkStatus: HistorikkStatus,

    @Schema(description = "Referanse til hvor den historiske revisjonen kan finnes")
    val href: String
)

@Schema(description = "Representasjon av en nasjon")
data class NasjonResponse(
    @Schema(description = "ID-en til nasjonen")
    val id: String,

    @Schema(description = "Liste over navn til nasjonen")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "LokalID til nasjonen")
    val lokalid: String,

    @Schema(description = "Navnerommet til nasjonen")
    val navnerom: String,

    @Schema(description = "Geometrien til nasjonen. Ikke grenser")
    val features: FeatureCollection
)

@Schema(description = "Representasjon av et fylke")
data class FylkeResponse(

    @Schema(description = "ID-en til fylket")
    val id: String,

    @Schema(description = "Liste over navn til fylket")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "LokalIDen til fylket")
    val lokalid: String,

    @Schema(description = "Navnerommet til fylket")
    val navnerom: String,

    @Schema(description = "Fylkesnummeret til fylket")
    val fylkesnummer: Fylkesnummer,

    @Schema(description = "Angir om fylket er et samisk forvaltningsområde eller ikke")
    val samiskforvaltningsomraade: Boolean,

    @Schema(description = "Angir når dette fylket ble sist oppdatert")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Geometrien til fylket. Ikke grenser")
    val features: FeatureCollection
)

@Schema(description = "")
data class KommuneResponse(
    @Schema(description = "ID-en til kommunen")
    val id: String,

    @Schema(description = "Liste over navn til kommunen")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "LokalIDen til kommunen")
    val lokalid: String,

    @Schema(description = "Navnerommet til kommunen")
    val navnerom: String,

    @Schema(description = "Kommunenummeret til kommunen")
    val kommunenummer: Kommunenummer,

    @Schema(description = "Angir om kommunen er et samisk forvaltningsområde eller ikke")
    val samiskforvaltningsomraade: Boolean,

    @Schema(description = "Angir når denne kommunen ble sist oppdatert")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Geometrien til kommunen. Ikke grenser")
    val features: FeatureCollection
)

@Schema(description = "Representasjon av et kommunenummer")
data class Kommunenummer(

    @Schema(description = "Unik UUID for kommunenummeret")
    val id: String,

    @Schema(description = "Det faktiske kommunenummeret")
    val kodeverdi: Int?
)
