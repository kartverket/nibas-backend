package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.common.geojson.Identifikasjon
import no.kartverket.nibas.inndelinger.GrenseType
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory

class FeatureBuilder(private val id: String? = "1") {
    companion object {
        val DEFAULT_FEATURE = FeatureBuilder().build()
    }

    private var grenseType: GrenseType = GrenseType.Kommunegrense
    private var lokalid: String = "123"
    private var navnerom: String = "ABC"
    private var kontekstId: String = "123"
    private var kontekstType: AdministrativEnhetType = AdministrativEnhetType.KOMMUNE
    private var geometry: Geometry =
        GeometryFactory().createLineString(arrayOf(Coordinate(0.0, 0.0), Coordinate(1.0, 1.0)))

    fun build() = Feature(
        id = id,
        properties = FeatureProperties(
            type = grenseType.toString(),
            identifikasjon = Identifikasjon(lokalid, navnerom),
            kontekstEgenskaper = KontekstEgenskaper(
                id = kontekstId,
                type = kontekstType
            )
        ),
        geometry = geometry
    )

    fun withGrensetype(grenseType: GrenseType): FeatureBuilder {
        this.grenseType = grenseType
        return this
    }

    fun withKontekstId(kontekstId: String): FeatureBuilder {
        this.kontekstId = kontekstId
        return this
    }

    fun withKontekstType(kontekstType: AdministrativEnhetType): FeatureBuilder {
        this.kontekstType = kontekstType
        return this
    }

    fun withGeometry(geometry: Geometry): FeatureBuilder {
        this.geometry = geometry
        return this
    }
}
