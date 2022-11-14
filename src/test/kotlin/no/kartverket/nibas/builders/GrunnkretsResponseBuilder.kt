package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.inndelinger.flateToFeature
import no.kartverket.nibas.inndelinger.posisjonToFeature
import no.kartverket.nibas.kodelister.KodelisteService

class GrunnkretsResponseBuilder(private val kodelisteService: KodelisteService) {

    private var grunnkrets: Grunnkrets = GrunnkretsBuilder().build()
    private var kommunenummer: String = "1787"
    private var version: Int = 1

    fun forGrunnkrets(grunnkrets: Grunnkrets): GrunnkretsResponseBuilder {
        this.grunnkrets = grunnkrets
        return this
    }

    fun build(): GrunnkretsResponse {
        return GrunnkretsResponse(
            id = grunnkrets.lokalid,
            navn = grunnkrets.grunnkretsnavn!!,
            grunnkretsnummer = grunnkrets.grunnkretsnummer!!,
            identifikasjon = Identifikasjon(grunnkrets.lokalid),
            kommunenummer = Kommunenummer(grunnkrets.kommune.kommunenummer, kommunenummer),
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
