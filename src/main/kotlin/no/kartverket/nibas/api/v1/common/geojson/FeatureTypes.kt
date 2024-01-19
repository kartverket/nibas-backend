package no.kartverket.nibas.api.v1.common.geojson

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.metadata.Metadata
import no.kartverket.nibas.api.v1.response.ObjektIdentifikator
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import jakarta.validation.constraints.NotNull

const val DISCRIMINATOR_FIELD: String = "type"
const val POINT_SCHEMA: String = "Point"
const val LINE_STRING_SCHEMA: String = "LineString"
const val MULTI_POLYGON_SCHEMA: String = "MultiPolygon"

@Schema(
    description = "Geometry-typene vi støtter i NIBAS.",
    subTypes = [
        Point::class,
        LineString::class,
        MultiPolygon::class
    ],
    discriminatorProperty = DISCRIMINATOR_FIELD,
    discriminatorMapping = [
        DiscriminatorMapping(value = POINT_SCHEMA, schema = Point::class),
        DiscriminatorMapping(value = LINE_STRING_SCHEMA, schema = LineString::class),
        DiscriminatorMapping(value = MULTI_POLYGON_SCHEMA, schema = MultiPolygon::class)
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = DISCRIMINATOR_FIELD)
@JsonSubTypes(
    JsonSubTypes.Type(value = Point::class, name = POINT_SCHEMA),
    JsonSubTypes.Type(value = LineString::class, name = LINE_STRING_SCHEMA),
    JsonSubTypes.Type(value = MultiPolygon::class, name = MULTI_POLYGON_SCHEMA)
)
interface Geometry

@Schema(
    name = POINT_SCHEMA,
    description = "Wrapper-objekt rundt en JTS Point.",
)
data class Point(
    @Schema(description = "Geometriens type. Diskriminator.")
    val type: String = POINT_SCHEMA,
    @Schema(description = "X og y som liste.")
    val coordinates: List<Double>)
    : Geometry

@Schema(
    name = LINE_STRING_SCHEMA,
    description = "Wrapper-objekt rundt en JTS LineString.",
)
data class LineString(
    @Schema(description = "Geometriens type. Diskriminator.")
    val type: String = LINE_STRING_SCHEMA,
    @Schema(description = "Liste av (x og y som liste).")
    val coordinates: List<List<Double>>)
    : Geometry

@Schema(
    name = MULTI_POLYGON_SCHEMA,
    description = "Wrapper-objekt rundt et JTS MultiPolygon.",
)
data class MultiPolygon(
    @Schema(description = "Geometriens type. Diskriminator.")
    val type: String = MULTI_POLYGON_SCHEMA,
    @Schema(description = "Liste av polygonenes liste av LinearRings som igjen har liste av koordinater som er liste av x og y.")
    val coordinates: List<List<List<List<Double>>>>)
    : Geometry

data class FeatureCollection(
    @Schema(description = "Toppnivå for geojson-strukturen, definert av en konstant med navn type og verdi FeatureCollection")
    val type: String = "FeatureCollection",

    @Schema(description = "Liste av features som holder på dataene")
    val features: List<Feature>
)

data class Feature(
    @Schema(description = "En konstant, navn: type, verdi: Feature")
    val type: String = "Feature",

    @Schema(description = "Id til feature")
    val id: String? = null,

    @Schema(description = "Et sett med egenskaper som beskriver denne featuren")
    @JsonProperty("properties")
    var properties: FeatureProperties,

    @Schema(description = "Geometrien til denne featuren.")
    var geometry: Geometry
)

@Schema(description = "Egenskaper som beskriver en gitt feature (properties-objektet i geojson-strukturen)")
data class FeatureProperties(

    @Schema(description = "Avhengig av hva slags feature man har kan dette være enten grensetype eller geometri-type (posisjon eller flate)")
    val type: String,

    @Schema(description = "Koordinatsystemet. Defaulter til 25833.")
    val srid: Int = SosiGeometryFunctions.defaultSrid,

    @Schema(
        description = "Diverse metadata-felter fra sosi-modellen.",
    )
    val metadata: Metadata? = null,

    @Schema(description = "Egenskaper til konteksten til grensen.")
    val kontekstEgenskaper: List<KontekstEgenskaper>? = listOf(),

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int,

    @Schema(description = "Egenskap som sier om en grense med en grenseendring er skal arkiveres")
    val shouldArchive: Boolean = false
)

@Schema(description = "Egenskaper som beskriver konteksten som grensen sees i.")
data class KontekstEgenskaper(
    @Schema(description = "Id til objekt som det vises til i kontekst, er null hvis feature skal knyttes til en ny krets")
    val id: ObjektIdentifikator? = null,

    @Schema(description = "Id til kommunen som konteksten ligger i")
    val kommuneId: ObjektIdentifikator? = null,

    @Schema(description = "kretsnummeret til konteksten")
    val kretsNummer: String? = null,

    @Schema(description = "Hvilken kontekst geometrien skal sees i")
    val type: KontekstEnhetType? = null,

    @Schema(description = "Teknisk versjon til referert objekt for å støtte samhandling og redigering")
    @NotNull
    val version: Int,

    @Schema(description = "Angir om grensen skal leses med klokken for å danne flate.")
    val retningMedKlokken: Boolean = true,

    @Schema(description = "Angir rekkefølgen på gitt grenselinje i flatedanningen.")
    val rekkefoelge: Int = 0,

    @Schema(description = "Angir hvilken flate for enheten grensen er en del av.")
    val flateIndeks: Int = 0,

    @Schema(description = "Angir om grensen er en del av et hull, og i så fall index på gitt hull.")
    val hullIndeks: Int? = null
)
