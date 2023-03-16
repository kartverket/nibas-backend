package no.kartverket.nibas.extensions.geometry

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.metadata.MetadataMapper
import no.kartverket.nibas.api.v1.response.ObjektIdentifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import java.time.LocalDate
import no.kartverket.nibas.api.v1.common.geojson.Geometry as GeometryDto
import no.kartverket.nibas.api.v1.common.geojson.LineString as LineStringDto
import no.kartverket.nibas.api.v1.common.geojson.MultiPolygon as MultiPolygonDto
import no.kartverket.nibas.api.v1.common.geojson.Point as PointDto

fun GeometryDto.toLineString(srid: Number): LineString {
    if (this !is LineStringDto) {
        throw RuntimeException("Geometry kan ikke utledes til en LineString. Geometry var av type: $this")
    }
    return SosiGeometryFunctions.koordinaterToLineString(this.coordinates, srid.toString())
}

fun Geometry.geometryToDto(): GeometryDto {
    return when (this) {
        is Point -> this.posisjonToDto()
        is LineString -> this.lineStringToDto()
        is MultiPolygon -> this.multiPolygonToDto()
        else -> throw RuntimeException("Ukjent type Geometry: $this")
    }
}

fun Point.posisjonToDto() = PointDto(coordinates = listOf(this.x, this.y))
fun Point?.posisjonToFeature(version: Int): Feature =
    Feature(
        properties = FeatureProperties(type = "Posisjon", srid = this!!.srid, version = version),
        geometry = posisjonToDto()
    )

fun LineString.lineStringToDto() = LineStringDto(
    coordinates = this.coordinates.map { listOf(it.x, it.y) })

fun MultiPolygon.multiPolygonToDto(): MultiPolygonDto {
    val listOfPolygons: MutableList<List<List<List<Double>>>> = mutableListOf()
    getPolygons().forEach { polygon ->
        val listOfRings: MutableList<List<List<Double>>> = mutableListOf()
        polygon.getRings()
            .forEach { linearRing ->
                val listOfCoordinates: MutableList<List<Double>> = mutableListOf()
                linearRing.coordinates.toList()
                    .forEach { coordinate ->
                        val listOfXAndY = listOf(coordinate.x, coordinate.y)
                        listOfCoordinates.add(listOfXAndY)
                    }
                listOfRings.add(listOfCoordinates)
            }
        listOfPolygons.add(listOfRings)
    }
    return MultiPolygonDto(
        coordinates = listOfPolygons
    )
}

private fun MultiPolygon.getPolygons(): List<Polygon> {
    val polygons = mutableListOf<Polygon>()
    for (i in 0 until this.numGeometries) {
        polygons.add(this.getGeometryN(i) as Polygon)
    }
    return polygons
}

private fun Polygon.getRings(): List<LinearRing> {
    val rings = mutableListOf<LinearRing>()
    rings.add(this.exteriorRing)
    for (i in 0 until this.numInteriorRing) {
        rings.add(this.getInteriorRingN(i))
    }
    return rings
}

fun Grense.grenseToFeature(): Feature {
    val grensetype = this.grensetype
    var featureProperties = FeatureProperties(type = grensetype.value, srid = this.grense.srid, version = this.version)
    val metadataMapper = MetadataMapper()
    featureProperties = when (grensetype) {
        Grensetype.Fylkesgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForAdministrativGrense(this))
        Grensetype.Kommunegrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForAdministrativGrense(this))
        Grensetype.Riksgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForRiksgrense(this))
        Grensetype.AvtaltAvgrensningslinje -> featureProperties.copy(metadata = metadataMapper.toMetadataForAvtaltAvgrensningslinje(this))
        Grensetype.Territorialgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForTerritorialgrense(this))
        Grensetype.Grunnlinje -> featureProperties.copy(metadata = metadataMapper.toMetadataForGrunnlinje(this))
        Grensetype.Delomraadegrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForStatistiskGrense(this))
        Grensetype.Grunnkretsgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForStatistiskGrense(this))
        Grensetype.Stemmekretsgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForKommunalKretsgrense(this))
        else -> throw RuntimeException("Ikke støttet å mappe til metadata for grensetype: " + grensetype.name)
    }
    return Feature(
        // Feature må ha en id, men denne må være objektets lokalid
        id = this.lokalid,
        properties = featureProperties,
        geometry = this.grense.lineStringToDto()
    )
}

fun Flate.flateToFeature(version: Int): Feature {
    var featureProperties = FeatureProperties(
        type = type.name,
        srid = omraade?.srid ?: SosiGeometryFunctions.defaultSrid,
        version = version
    )
    val metadataMapper = MetadataMapper()

    featureProperties = featureProperties.copy(metadata = metadataMapper.toMetadata(this))

    return Feature(
        // Feature må ha en id, men denne må være objektets lokalid
        id = lokalid,
        properties = featureProperties,
        geometry = omraade?.multiPolygonToDto() ?: no.kartverket.nibas.api.v1.common.geojson.MultiPolygon(coordinates = emptyList()),
    )
}

/**
 * Default angitt kontekst er Grunnkrets.
 * Kontekstegenskapene retning, rekkefoelge, flateIndeks, hullIndeks gir nå kun mening for
 * Grunnkrets/Stemmekrets.
 * Metoden tar inn gyldighetsdato, defaulter til dagens dato. Dette for å kunne velge rett versjon av Grense-objektet dersom det finnes framtidige versjoner.
 */
fun GrenseKobling.grenseKoblingToFeature(
    kontekstType: KontekstEnhetType = KontekstEnhetType.GRUNNKRETS,
    gyldighetsdato: LocalDate = LocalDate.now()
): Feature {
    val feature = getRelevantGrense(gyldighetsdato).grenseToFeature()
    val koblingAttributter = grenseKoblingAttributter
    return feature.copy(
        properties = feature.properties.copy(
            kontekstEgenskaper = KontekstEgenskaper(
                id = ObjektIdentifikator(
                    lokalid = Lokalid(getEnhetKobletTilGrense().lokalid),
                    gyldighetsdato = gyldighetsdato
                ),
                type = kontekstType,
                retningMedKlokken = koblingAttributter.retningMedKlokken,
                rekkefoelge = koblingAttributter.rekkefoelge,
                flateIndeks = koblingAttributter.flateIndeks,
                hullIndeks = koblingAttributter.hullIndeks
            )
        )
    )
}
