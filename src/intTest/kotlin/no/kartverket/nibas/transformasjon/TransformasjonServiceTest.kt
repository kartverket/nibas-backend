package no.kartverket.nibas.transformasjon

import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKTReader
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertTrue

class TransformasjonServiceTest : TestWithDbContainer() {

    @Autowired
    lateinit var transformasjonService: TransformasjonService

    @Test
    fun testKjorendePostgisContainer() {
        val geometryFactory = GeometryFactory(PrecisionModel(), 25833)
        val wktReader = WKTReader(geometryFactory)

        val testWkt = "POINT(6647529 261055)"

        val geometry: Geometry = wktReader.read(testWkt)
        val transformertGeometry: Geometry = transformasjonService.transformer(geometry, 4258)

        assertTrue(geometry.toText() != transformertGeometry.toText())
    }
}
