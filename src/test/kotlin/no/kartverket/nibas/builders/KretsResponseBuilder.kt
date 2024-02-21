package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.ekstern.response.EksternKretsResponse
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.extensions.geometry.flateToFeature
import no.kartverket.nibas.extensions.geometry.posisjonToFeature

class KretsResponseBuilder(
    private var grunnkrets: Grunnkrets = GrunnkretsBuilder().build(),
    private var stemmekrets: Stemmekrets = StemmekretsBuilder().build(),
    private var kommunenummer: String = "0301"
) {

    fun buildForGrunnkrets(): EksternKretsResponse {
        return EksternKretsResponse(
            id = grunnkrets.lokalid,
            navn = grunnkrets.grunnkretsnavn!!,
            nummer = grunnkrets.grunnkretsnummer!!,
            kommunenummer = Kommunenummer(grunnkrets.getKommune(grunnkrets.gyldighet.gyldigFra)!!.kommunenummerUuid, kommunenummer),
            features = FeatureCollection(
                features = listOf(
                    grunnkrets.posisjon.posisjonToFeature(grunnkrets.version),
                    grunnkrets.flateToFeature(grunnkrets.version)
                )
            ),
        )
    }

    fun buildForStemmekrets(): EksternKretsResponse {
        return EksternKretsResponse(
            id = stemmekrets.lokalid,
            navn = stemmekrets.stemmekretsnavn!!,
            nummer = stemmekrets.stemmekretsnummer!!,
            kommunenummer = Kommunenummer(stemmekrets.getKommune(stemmekrets.gyldighet.gyldigFra)!!.kommunenummerUuid, kommunenummer),
            features = FeatureCollection(
                features = listOf(
                    stemmekrets.posisjon.posisjonToFeature(stemmekrets.version),
                    stemmekrets.flateToFeature(stemmekrets.version)
                )
            ),
        )
    }
}
