package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.metadata.AdministrativGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.AvtaltAvgrensningslinjeMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.CommonMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.DokumentasjonsreferanseDTO
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.KommunalKretsgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.Metadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.RiksgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.Sporingsinformasjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.StatistiskgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.TerritorialgrenseMetadata
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.geometry.defaultSrid
import no.kartverket.nibas.extensions.geometry.geometryToDto
import no.kartverket.nibas.extensions.geometry.toLineString
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate
import java.time.ZonedDateTime

class FeatureBuilder(private var id: String? = "1") {

    private var grenseType: Grensetype = Grensetype.Kommunegrense
    private var grenseVersjon = 1
    private var metadata: Metadata? = null
    private var lokalid: String? = "123"
    private var kontekstEgenskaper: List<KontekstEgenskaper> = listOf(
        KontekstEgenskaper(ObjektIdentifikator(Lokalid("1"), LocalDate.now()), version = 1, type = KontekstEnhetType.GRUNNKRETS),
        KontekstEgenskaper(ObjektIdentifikator(Lokalid("2"), LocalDate.now()), version = 1, type = KontekstEnhetType.GRUNNKRETS)
    )
    private var srid: Int = defaultSrid
    private var geometry: Geometry =
        GeometryFactory().createLineString(arrayOf(Coordinate(0.0, 0.0), Coordinate(1.0, 1.0)))
    private var shouldArchive: Boolean = false
    private val dokumentasjonsreferanser = mutableListOf<DokumentasjonsreferanseDTO>()

    fun build(): Feature {
        var properties = FeatureProperties(
            type = grenseType.value,
            srid = srid,
            version = grenseVersjon,
            kontekstEgenskaper = kontekstEgenskaper,
            shouldArchive = shouldArchive
        )

        if (metadata == null) {
            val commonMetadata = CommonMetadata(
                identifikasjon = Identifikasjon(lokalid ?: "NYFEATURE"),
                sporingsinformasjon = Sporingsinformasjon(oppdateringsdato = ZonedDateTime.now()),
                gyldigFra = LocalDate.now()
            )
            metadata = when (grenseType) {
                Grensetype.Kommunegrense -> AdministrativGrenseMetadata(common = commonMetadata, dokumentasjonsreferanser = dokumentasjonsreferanser)
                Grensetype.Fylkesgrense -> AdministrativGrenseMetadata(common = commonMetadata, dokumentasjonsreferanser = dokumentasjonsreferanser)
                Grensetype.Riksgrense -> RiksgrenseMetadata(common = commonMetadata, dokumentasjonsreferanser = dokumentasjonsreferanser)
                Grensetype.Territorialgrense -> TerritorialgrenseMetadata(common = commonMetadata, dokumentasjonsreferanser = dokumentasjonsreferanser)
                Grensetype.AvtaltAvgrensningslinje -> AvtaltAvgrensningslinjeMetadata(common = commonMetadata)
                Grensetype.Stemmekretsgrense -> KommunalKretsgrenseMetadata(common = commonMetadata)
                Grensetype.Grunnkretsgrense -> StatistiskgrenseMetadata(common = commonMetadata)
                Grensetype.Delomraadegrense -> StatistiskgrenseMetadata(common = commonMetadata)
                else -> throw RuntimeException("Not implemented yet.")
            }
        }
        properties = properties.copy(metadata = metadata)
        if (grenseType == Grensetype.Kommunegrense) {
            (metadata as AdministrativGrenseMetadata).dokumentasjonsreferanser = dokumentasjonsreferanser
        }

        return Feature(
            id = id,
            properties = properties,
            geometry = geometry.geometryToDto()
        )
    }

    // Kan brukes for å legge til nye ting på en eksisterende Feature man har.
    // Så slipper man å gjøre alt i en transformasjon, og kan ha mer generelle funksjoner i testkoden.
    fun copy(feature: Feature): FeatureBuilder {
        this.grenseType = when (feature.properties.type) {
            "Stemmekretsgrense" -> Grensetype.Stemmekretsgrense
            "Grunnkretsgrense" -> Grensetype.Grunnkretsgrense
            "Kommunegrense" -> Grensetype.Kommunegrense
            else -> throw RuntimeException("Mangler støtte for ${feature.properties.type}, legg til dette i testkoden")
        }
        this.kontekstEgenskaper = feature.properties.kontekstEgenskaper
        this.grenseVersjon = feature.properties.version
        this.metadata = feature.properties.metadata
        this.lokalid = feature.id ?: throw RuntimeException("Mangler lokalid")
        this.geometry = feature.geometry.toLineString(this.srid)
        return this
    }

    fun withGrensetype(grenseType: Grensetype): FeatureBuilder {
        this.grenseType = grenseType
        return this
    }

    fun withKontekstEgenskaper(kontekstEgenskaper: List<KontekstEgenskaper>): FeatureBuilder {
        this.kontekstEgenskaper = kontekstEgenskaper
        return this
    }

    fun withGrenseversjon(grenseversjon: Int): FeatureBuilder {
        this.grenseVersjon = grenseversjon
        return this
    }

    fun withGeometry(geometry: Geometry): FeatureBuilder {
        this.geometry = geometry
        return this
    }

    fun withLokalid(lokalid: String?): FeatureBuilder {
        this.id = lokalid
        this.lokalid = lokalid
        return this
    }

    fun withVersion(version: Int): FeatureBuilder {
        this.grenseVersjon = version
        return this
    }

    fun withDokref(dokref: List<DokumentasjonsreferanseDTO>): FeatureBuilder {
        dokumentasjonsreferanser.addAll(dokref)
        return this
    }
}
