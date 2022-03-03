package no.kartverket.nibas.api.v1.common.geojson

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import org.locationtech.jts.geom.Geometry

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

    @JsonSerialize(using = NibasGeometryAsGeoJSONSerializer::class)
    @JsonDeserialize(using = NibasGeometryDeserializer::class)
    @Schema(description = "Geometrien til denne featuren.")
    var geometry: Geometry
)

@Schema(description = "Egenskaper som beskriver en gitt feature (properties-objektet i geojson-strukturen)")
data class FeatureProperties(

    @Schema(description = "Definerer hvilken type geometri som leveres")
    val type: String,

    //TODO: Lar denne ligge her midlertidig, bør flyttes inn i metadata-type også for de ikke-adm.grensene
    @Schema(description = "Identifikasjon for grensen i sosi-modellen.")
    val identifikasjon: Identifikasjon? = null,

    @Schema(description = "Diverse metadata-felter fra sosi-modellen.")
    val metadata: AdmGrenserMetadata? = null,

    @Schema(description = "Egenskaper til konteksten til grensen.")
    val contextProperties: ContextProperties? = null
)

@Schema(description = "Egenskaper som beskriver konteksten som grensen sees i.")
data class ContextProperties (
    @Schema(description = "Id til objekt som det vises til i kontekst")
    val id: String? = null,

    @Schema(description = "Hvilken kontekst geometrien skal sees i")
    val type: AdministrativEnhetType? = null,

    @Schema(description = "Angir om grensen skal leses mot klokken for å danne flate.")
    val directionClockwise: Boolean = true,

    @Schema(description = "Angir rekkefølgen på gitt grenselinje i flatedanningen.")
    val order: Int = 0,

    @Schema(description = "Angir om grensen er en del av et hull, og i så fall index på gitt hull.")
    val holeIndex: Int? = null
)
