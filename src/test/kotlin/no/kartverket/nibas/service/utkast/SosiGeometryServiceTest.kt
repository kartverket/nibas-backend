package no.kartverket.nibas.service.utkast

import io.mockk.every
import io.mockk.mockk
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.metadata.KommunalKretsgrenseMetadata
import no.kartverket.nibas.common.geometry.avrundLineString
import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.geometry.toMultiPolygon
import no.kartverket.nibas.extensions.geometry.toLineString
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.sosi.model.SosiObjekt
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.operation.polygonize.Polygonizer
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class SosiGeometryServiceTest {

    private val sosiGeometryService = SosiGeometryService(kodelisteService = mockk(relaxed = true))

    // KVALITET i fila blir til en posisjonskvalitet med href, og href bygges fra requesten
    @BeforeEach
    fun settRequest() = RequestContextHolder.setRequestAttributes(ServletRequestAttributes(MockHttpServletRequest()))

    @AfterEach
    fun fjernRequest() = RequestContextHolder.resetRequestAttributes()

    // Uten omprojisering er arealene kjente: 2 x 10 000 m² og en halvsirkel delt i 64 linjestykker, 3 925 m²
    private fun lesUtenOmprojisering() =
        sosiGeometryService.getGeoJsonForSosiObjekt(BopliktSosiTestdata.les(), "EPSG:25832").features

    private fun linjer(features: List<Feature>) = features.map { it.geometry.toLineString(it.properties.srid) }

    @Test
    fun `kurver med grensetype NIBAS ikke kjenner blir avvist`() {
        assertThatThrownBy { sosiGeometryService.getGeoJsonForSosiObjekt(BopliktSosiTestdata.les("Bopliktomr?degrense"), "EPSG:25833") }
            .hasMessageContaining("Bopliktomr?degrense")
    }

    @Test
    fun `grensetypen kommer fra fila`() {
        val features = sosiGeometryService.getGeoJsonForSosiObjekt(BopliktSosiTestdata.les(), "EPSG:25833").features

        assertThat(features.map { it.properties.type }).containsOnly(Grensetype.Bopliktomraadegrense.value)
    }

    @Test
    fun `koordinater blir avrundet til naermeste halve cm etter omprojisering`() {
        val features = sosiGeometryService.getGeoJsonForSosiObjekt(BopliktSosiTestdata.les(), "EPSG:25833").features

        features.forEach { feature ->
            val koordinater = feature.geometry.toLineString(feature.properties.srid).coordinates
            assertThat(avrundLineString(koordinater)).isEqualTo(koordinater)
        }
    }

    @Test
    fun `buer blir med som linjer`() {
        assertThat(lesUtenOmprojisering()).hasSize(7 + 1)
    }

    @Test
    fun `buer henger sammen med nabolinjene slik at alle flatene lukkes`() {
        val polygonizer = Polygonizer().apply { add(linjer(lesUtenOmprojisering())) }

        @Suppress("UNCHECKED_CAST")
        val flater = polygonizer.polygons as Collection<Polygon>

        // Uten buen lukkes bare A og B
        assertThat(flater).hasSize(2)
        assertThat(flater.sumOf { it.area }).isBetween(23_920.0, 23_930.0)
        assertThat(polygonizer.dangles).isEmpty()
    }

    @Test
    fun `linjene kan settes sammen til flater slik NIBAS gjoer ved publisering`() {
        val omraade = getGrenserIRiktigRekkefoelge(linjer(lesUtenOmprojisering())).toMultiPolygon()

        assertThat(omraade.isValid).isTrue()
        // A og B blir én flate, C en annen
        assertThat(omraade.numGeometries).isEqualTo(2)
        assertThat(omraade.area).isBetween(23_920.0, 23_930.0)
    }

    @Test
    fun `SOSI 5_0 med maalemetode og noeyaktighet som egne linjer under KVALITET`() {
        assertKvalitet82Med50(BopliktSosiTestdata.les(sosiVersjon = "5.0"))
    }

    @Test
    fun `SOSI 5_0 med maalemetode og noeyaktighet i hermetegn paa KVALITET-linjen`() {
        assertKvalitet82Med50(BopliktSosiTestdata.les(sosiVersjon = "5.0", kvalitet = listOf("..KVALITET \"82 50\"")))
    }

    private fun assertKvalitet82Med50(sosiObjekt: SosiObjekt) {
        val kodelisteService = mockk<KodelisteService> { every { getMalemetodeIdByCodeValue("82") } returns "maalemetode-82" }

        val features = SosiGeometryService(kodelisteService = kodelisteService)
            .getGeoJsonForSosiObjekt(sosiObjekt, "EPSG:25833")
            .features

        val kvaliteter = features.map { (it.properties.metadata as KommunalKretsgrenseMetadata).commonGrense.posisjonskvalitet }
        assertThat(kvaliteter).hasSize(8).allSatisfy {
            assertThat(it?.maalemetode?.id).isEqualTo("maalemetode-82")
            assertThat(it?.noeyaktighet).isEqualTo(50)
        }
    }
}
