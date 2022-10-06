package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.inndelinger.flateToFeature
import no.kartverket.nibas.inndelinger.posisjonToFeature
import no.kartverket.nibas.kodelister.KodelisteService

class GrunnkretsResponseBuilder(private val kodelisteService: KodelisteService) {

    private var grunnkrets: Grunnkrets = GrunnkretsBuilder().build()
    private var version: Int = 1

    fun forGrunnkrets(grunnkrets: Grunnkrets): GrunnkretsResponseBuilder {
        this.grunnkrets = grunnkrets
        return this
    }

    fun build(): GrunnkretsResponse {
        return GrunnkretsResponse(
            id = grunnkrets.id,
            navn = grunnkrets.grunnkretsnavn!!,
            grunnkretsnummer = grunnkrets.grunnkretsnummer!!,
            identifikasjon = Identifikasjon(grunnkrets.lokalid, grunnkrets.navnerom, grunnkrets.versjonid),
            kommunenummer = grunnkrets.kommunenummer!!,
            features = FeatureCollection(
                features = listOf(
                    posisjonToFeature(grunnkrets.posisjon, grunnkrets.version),
                    flateToFeature(grunnkrets, kodelisteService, grunnkrets.version)
                )
            ),
            version = version
        )
    }

}
