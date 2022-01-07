package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.inndelinger.GrenseType
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

class FeatureBuilder(private val id: Int = 1) {
    companion object {
        val DEFAULT_FEATURE = FeatureBuilder().build()
    }

    private var grenseType: GrenseType = GrenseType.Kommunegrense
    private var lokalid: String = "123"
    private var navnerom: String = "ABC"
    private var kontekstId: Int = 123
    private var kontekstType: AdministrativEnhetType = AdministrativEnhetType.KOMMUNE

    fun build() = Feature(
        properties = FeatureProperties(
            type = grenseType.toString(),
            id = id,
            lokalid = lokalid,
            navnerom = navnerom,
            kontekstId = kontekstId,
            kontekstType = kontekstType
        ),
        geometry = GeometryFactory().createPoint(Coordinate(0.0, 0.0))
    )

    fun withGrensetype(grenseType: GrenseType): FeatureBuilder {
        this.grenseType = grenseType
        return this
    }
}