package no.kartverket.nibas.api.v1

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.LineString
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import kotlin.test.assertTrue
import kotlin.test.fail

class KommuneApiIntegrationTest {

    @Test
    fun thatFlatedanningErMuligMedHull() {
        val stjordal = "9cc5760c-556b-49b0-90ed-744654caf8ea" // Finn korrekt id i egen DB
        beregnFlateForKommuneId(stjordal)
    }

    @Test
    fun thatFlatedanningErMuligMedFlereFlater() {
        val malvik = "00205fb8-78e9-4c29-9307-30fd4ccf3be9" // Finn korrekt id i egen DB
        beregnFlateForKommuneId(malvik)
    }

    fun beregnFlateForKommuneId(id: String) {
        val webClient = WebClient.builder().baseUrl("http://localhost:8080/v1")
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }.build()

        val featureCollection = webClient.get()
            .uri("/kommuner/${id}/grenser")
            .retrieve()
            .bodyToMono(FeatureCollection::class.java)
            .block(Duration.ofMillis(5_000))

        when (featureCollection) {
            null -> fail("Could not get result")
            else -> {
                val grenseKoblinger = featureCollection.features.map { featureToGrenseKobling(it) }
                assertTrue(SosiGeometryFunctions.beregnFlate(grenseKoblinger.toSet()).isValid)

            }
        }
    }

    fun featureToGrenseKobling(feature: Feature): GrenseKobling {
        // FYI, bryr oss ikke om hvilken type grense det er, skal bare verifisere at polygoner blir laget riktig
        val ke = feature.properties.kontekstEgenskaper[0]

        val grunnkretsgrense = Grense(grensetype = Grensetype.Grunnkretsgrense)
        grunnkretsgrense.grense = feature.geometry as LineString

        return GrunnkretsGrenseKobling(
            Grunnkrets(),
            grunnkretsgrense,
            GrenseKoblingAttributter(ke.retningMedKlokken, ke.rekkefoelge, ke.flateIndeks, ke.hullIndeks)
        )
    }
}
