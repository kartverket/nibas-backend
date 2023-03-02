package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.service.flateToFeature
import no.kartverket.nibas.service.posisjonToFeature

class KretsResponseBuilder(
    private var grunnkrets: Grunnkrets = GrunnkretsBuilder().build(),
    private var stemmekrets: Stemmekrets = StemmekretsBuilder().build(),
    private var kommunenummer: String = "0301"
) {

    fun buildForGrunnkrets(): KretsResponse {
        return KretsResponse(
            id = grunnkrets.lokalid,
            navn = grunnkrets.grunnkretsnavn!!,
            nummer = grunnkrets.grunnkretsnummer!!,
            kommunenummer = Kommunenummer(grunnkrets.kommune.kommunenummer, kommunenummer),
            features = FeatureCollection(
                features = listOf(
                    posisjonToFeature(grunnkrets.posisjon, grunnkrets.version),
                    flateToFeature(grunnkrets, grunnkrets.version)
                )
            ),
        )
    }

    fun buildForStemmekrets(): KretsResponse {
        return KretsResponse(
            id = stemmekrets.lokalid,
            navn = stemmekrets.stemmekretsnavn!!,
            nummer = stemmekrets.stemmekretsnummer!!,
            kommunenummer = Kommunenummer(stemmekrets.kommune.kommunenummer, kommunenummer),
            features = FeatureCollection(
                features = listOf(
                    posisjonToFeature(stemmekrets.posisjon, stemmekrets.version),
                    flateToFeature(stemmekrets, stemmekrets.version)
                )
            ),
        )
    }

}
