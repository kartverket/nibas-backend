package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.metadata.AdministrativGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.AvtaltAvgrensningslinjeMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.CommonMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.Metadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.RiksgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.TerritorialgrenseMetadata
import no.kartverket.nibas.inndelinger.GrenseType
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.time.ZonedDateTime

class FeatureBuilder(private val id: String? = "1") {
    companion object {
        val DEFAULT_FEATURE = FeatureBuilder().build()
    }

    private var grenseType: GrenseType = GrenseType.Kommunegrense
    private var metadata: Metadata? = null
    private var lokalid: String = "123"
    private var navnerom: String = "ABC"
    private var kontekstId: String = "123"
    private var kontekstType: KontekstEnhetType = KontekstEnhetType.KOMMUNE
    private var geometry: Geometry =
        GeometryFactory().createLineString(arrayOf(Coordinate(0.0, 0.0), Coordinate(1.0, 1.0)))

    fun build(): Feature {
        var properties = FeatureProperties(
            type = grenseType.toString(),
            kontekstEgenskaper = KontekstEgenskaper(
                id = kontekstId,
                type = kontekstType
            )
        )

        if (metadata == null) {
            val commonMetadata = CommonMetadata(
                identifikasjon = Identifikasjon(lokalid, navnerom),
                oppdateringsdato = ZonedDateTime.now().toLocalDate()
            )
            metadata = when (grenseType) {
                GrenseType.Kommunegrense -> AdministrativGrenseMetadata(common = commonMetadata)
                GrenseType.Fylkesgrense -> AdministrativGrenseMetadata(common = commonMetadata)
                GrenseType.Riksgrense -> RiksgrenseMetadata(common = commonMetadata)
                GrenseType.Territorialgrense -> TerritorialgrenseMetadata(common = commonMetadata)
                GrenseType.AvtaltAvgrensningslinje -> AvtaltAvgrensningslinjeMetadata(common = commonMetadata)
                else -> throw RuntimeException("Not implemented yet.")
            }
        }
        properties = properties.copy(metadata = metadata)

        return Feature(
            id = id,
            properties = properties,
            geometry = geometry
        )
    }

    fun withGrensetype(grenseType: GrenseType): FeatureBuilder {
        this.grenseType = grenseType
        return this
    }

    fun withKontekstId(kontekstId: String): FeatureBuilder {
        this.kontekstId = kontekstId
        return this
    }

    fun withKontekstType(kontekstType: KontekstEnhetType): FeatureBuilder {
        this.kontekstType = kontekstType
        return this
    }

    fun withGeometry(geometry: Geometry): FeatureBuilder {
        this.geometry = geometry
        return this
    }

    fun withMetadata(metadata: Metadata): FeatureBuilder {
        this.metadata = metadata
        return this
    }
}
