package no.kartverket.nibas.api.v1

import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import kotlin.test.assertTrue
import kotlin.test.fail

class KommuneApiIntegrationTest {

    @Test
    fun thatFlatedanningErMuligMedGittRekkefolgeOgHull() {
        val webClient = WebClient.builder().baseUrl("http://localhost:8080/v1")
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }.build()

        val stjordal = "5d45c59d-f33e-4a9e-aee1-ad945d6a1fe5"
        val featureCollection = webClient.get()
            .uri("/kommuner/${stjordal}/grenser")
            .retrieve()
            .bodyToMono(FeatureCollection::class.java)
            .block(Duration.ofMillis(5_000))

        when(featureCollection) {
            null -> fail("Could not get result")
            else -> {

                val skall = featureCollection.features.filter { it.properties.contextProperties!!.holeIndex == null }
                val hull: Map<Int?, List<Feature>> = featureCollection.features.filter { it.properties.contextProperties!!.holeIndex != null }
                    .groupBy { it.properties.contextProperties!!.holeIndex }

                val gf = GeometryFactory(PrecisionModel(), 25833)
                val skallCoordinates = skall.sortedBy { it.properties.contextProperties!!.order }.map { it ->
                    when(it.properties.contextProperties!!.directionClockwise) {
                        true -> gf.createLineString(it.geometry.coordinates)
                        false -> gf.createLineString(it.geometry.coordinates).reverse()
                    }
                }

                val hullCoordinates = hull.map { h ->
                    h.value.sortedBy { it.properties.contextProperties!!.order }.map { it ->
                        when (it.properties.contextProperties!!.directionClockwise) {
                            true -> gf.createLineString(it.geometry.coordinates)
                            false -> gf.createLineString(it.geometry.coordinates).reverse()
                        }
                    }
                }

                assertTrue(SosiGeometryFunctions.lagFlate(skallCoordinates, hullCoordinates, "23").isValid)
            }
        }
    }
}