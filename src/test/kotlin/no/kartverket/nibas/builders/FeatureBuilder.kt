package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Grensetype
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
import no.kartverket.nibas.api.v1.common.geojson.metadata.Sporingsinformasjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.StatistiskgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.TerritorialgrenseMetadata
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.time.ZonedDateTime

class FeatureBuilder(private val id: String? = "1") {
    companion object {
        val DEFAULT_FEATURE = FeatureBuilder().build()
    }

    private var grenseType: Grensetype = Grensetype.Kommunegrense
    private var metadata: Metadata? = null
    private var lokalid: String = "123"
    private var navnerom: String = "ABC"
    private var kontekstId: String = "123"
    private var kontekstType: KontekstEnhetType = KontekstEnhetType.GRUNNKRETS
    private var retningMedKlokka: Boolean = true
    private var rekkefoelge: Int = 0
    private var geometry: Geometry =
        GeometryFactory().createLineString(arrayOf(Coordinate(0.0, 0.0), Coordinate(1.0, 1.0)))

    fun build(): Feature {
        var properties = FeatureProperties(
            type = grenseType.toString(),
            kontekstEgenskaper = KontekstEgenskaper(
                id = kontekstId,
                type = kontekstType,
                retningMedKlokken = retningMedKlokka,
                rekkefoelge = rekkefoelge
            )
        )

        if (metadata == null) {
            val commonMetadata = CommonMetadata(
                identifikasjon = Identifikasjon(lokalid, navnerom),
                sporingsinformasjon = Sporingsinformasjon(oppdateringsdato = ZonedDateTime.now().toLocalDate())
            )
            metadata = when (grenseType) {
                Grensetype.Kommunegrense -> AdministrativGrenseMetadata(common = commonMetadata)
                Grensetype.Fylkesgrense -> AdministrativGrenseMetadata(common = commonMetadata)
                Grensetype.Riksgrense -> RiksgrenseMetadata(common = commonMetadata)
                Grensetype.Territorialgrense -> TerritorialgrenseMetadata(common = commonMetadata)
                Grensetype.AvtaltAvgrensningslinje -> AvtaltAvgrensningslinjeMetadata(common = commonMetadata)
                // TODO: Mangler Metadata for Stemmekretsgrense
                Grensetype.Stemmekretsgrense -> AdministrativGrenseMetadata(common = commonMetadata)
                Grensetype.Grunnkretsgrense -> StatistiskgrenseMetadata(common = commonMetadata)
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

    fun withGrensetype(grenseType: Grensetype): FeatureBuilder {
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

    fun withRetning(retningMedKlokka: Boolean): FeatureBuilder {
        this.retningMedKlokka = retningMedKlokka
        return this
    }

    fun withRekkefoelge(rekkefoelge: Int): FeatureBuilder {
        this.rekkefoelge = rekkefoelge
        return this
    }
}
