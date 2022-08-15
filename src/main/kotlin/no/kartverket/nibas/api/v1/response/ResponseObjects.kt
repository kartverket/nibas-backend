package no.kartverket.nibas.api.v1.response

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Fylkesnummer
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.NibasGeometryAsGeoJSONSerializer
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.inndelinger.RevisjonStatus
import org.locationtech.jts.geom.Point
import org.springframework.http.HttpStatus
import java.time.LocalDate
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

@Schema(description = "En referanse til en grunnkrets")
data class GrunnkretsRef(

    @Schema(description = "ID-en til grunnkretser")
    val id: String,

    @Schema(description = "Navnet på grunnkretser")
    val navn: String,

    @Schema(description = "URL til full representasjon av grunnkretsen")
    val href: String,

    @Schema(description = "Nummeret til grunnkretsen")
    val grunnkretsnummer: String,
)

@Schema(description = "En referanse til en stemmekrets")
data class StemmekretsRef(

    @Schema(description = "ID-en til stemmekretsen")
    val id: String,

    @Schema(description = "Navnet på stemmekretsen")
    val navn: String,

    @Schema(description = "URL til full representasjon av stemmekrets")
    val href: String
)

@Schema(description = "En referanse til en historisk revisjon av en administrativ enhet")
data class RevisjonRef(

    @Schema(description = "IDen til den historiske revisjonen")
    val revisjonId: Int,

    @Schema(description = "Når revisjonen ble opprettet")
    val revisjonDato: Date,

    @Schema(description = "Angir status for hva som medførte en ny revisjon")
    val revisjonStatus: RevisjonStatus,

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
    val oppdateringsdato: LocalDate,

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
    val oppdateringsdato: LocalDate,

    @Schema(description = "Geometrien til kommunen. Ikke grenser")
    val features: FeatureCollection
)

@Schema(description = "Representasjon av en grunnkrets")
data class GrunnkretsResponse(
    @Schema(description = "ID-en til grunnkretsen")
    val id: String,

    @Schema(description = "Navnet på grunnkretsen")
    val navn: String,

    @Schema(description = "Grunnkretsnummeret til grunnkretsen")
    val grunnkretsnummer: String,

    @Schema(description = "Identifikasjon av grunnkretsen")
    val identifikasjon: Identifikasjon,

    @Schema(description = "Kommunenummeret til grunnkretsen")
    val kommunenummer: String,

    @Schema(description = "Geometrien til grunnkretsen, dvs representasjonspunkt og flaten")
    val features: FeatureCollection
)

@Schema(description = "Representasjon av en stemmekrets")
data class StemmekretsResponse(
    @Schema(description = "ID-en til stemmekretsen")
    val id: String,

    @Schema(description = "Navnet på stemmekretsen")
    val stemmekretsnavn: String,

    @Schema(description = "Stemmekretsnummeret til stemmekretsen")
    val stemmekretsnummer: String,

    @Schema(description = "Identifikasjon av stemmekretsen")
    val identifikasjon: Identifikasjon,

    @Schema(description = "Kommunenummeret til stemmekretsen")
    val kommunenummer: String,

    @Schema(description = "Tellekretsnummer til stemmekretsen")
    var tellekretsnummer: String? = null,

    @Schema(description = "Tellekretsnavn til stemmekretsen")
    var tellekretsnavn: String? = null,

    @Schema(description = "Valgdistriktsnummer til stemmekretsen")
    var valgdistriktsnummer: String? = null,

    @Schema(description = "Geometrien til stemmekretsen, dvs representasjonspunkt og flaten")
    val features: FeatureCollection
)

@Schema(description = "Representasjon av et kommunenummer")
data class Kommunenummer(

    @Schema(description = "Unik UUID for kommunenummeret")
    val id: String,

    @Schema(description = "Det faktiske kommunenummeret")
    val kodeverdi: Int?
)

@Schema(description = "Respons-type for geometrifeil")
data class GeometriErrorResponse(

    @Schema(description = "HttpStatus for responsen")
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,

    @Schema(description = "Type geometriobjekt")
    val geometryType: String? = null,

    @Schema(description = "Valideringsfeil")
    val validationError: String? = null,

    @JsonSerialize(using = NibasGeometryAsGeoJSONSerializer::class)
    @Schema(description = "Beskriver lokasjonen for feilen")
    val feilLokasjon: Point,

    @Schema(description = "Generell beskrivelse")
    val message: String,
)
