package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.inndelinger.flateToFeature
import no.kartverket.nibas.inndelinger.posisjonToFeature
import no.kartverket.nibas.kodelister.KodelisteService

class KretsResponseBuilder(private val kodelisteService: KodelisteService,
                           private var grunnkrets: Grunnkrets = GrunnkretsBuilder().build(),
                           private var stemmekrets: Stemmekrets = StemmekretsBuilder().build(),
                           private var kommunenummer: String = "1787") {

    fun buildForGrunnkrets(): KretsResponse {
        val lokalid = grunnkrets.lokalid
        return KretsResponse(
            id = lokalid,
            navn = grunnkrets.grunnkretsnavn!!,
            nummer = grunnkrets.grunnkretsnummer!!,
            kommunenummer = Kommunenummer(grunnkrets.kommune.kommunenummer, kommunenummer),
            features = FeatureCollection(
                features = listOf(
                    posisjonToFeature(grunnkrets.posisjon, grunnkrets.version),
                    flateToFeature(grunnkrets, kodelisteService, grunnkrets.version)
                )
            ),
        )
    }

    fun buildForStemmekrets(): KretsResponse {
        return KretsResponse(
            id = stemmekrets.id,
            navn = stemmekrets.stemmekretsnavn!!,
            nummer = stemmekrets.stemmekretsnummer!!,
            kommunenummer = Kommunenummer(stemmekrets.kommune.kommunenummer, kommunenummer),
            features = FeatureCollection(
                features = listOf(
                    posisjonToFeature(stemmekrets.posisjon, stemmekrets.version),
                    flateToFeature(stemmekrets, kodelisteService, stemmekrets.version)
                )
            ),
        )
    }

}
