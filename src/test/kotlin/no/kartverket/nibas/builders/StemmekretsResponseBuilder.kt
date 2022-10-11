package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.inndelinger.flateToFeature
import no.kartverket.nibas.inndelinger.posisjonToFeature
import no.kartverket.nibas.kodelister.KodelisteService

class StemmekretsResponseBuilder(private val kodelisteService: KodelisteService) {

    private var kommunenummer: String = "1787"
    private var stemmekrets: Stemmekrets = StemmekretsBuilder().build()
    private var version: Int = 1

    fun forStemmekrets(stemmekrets: Stemmekrets): StemmekretsResponseBuilder {
        this.stemmekrets = stemmekrets
        return this
    }

    fun build(): StemmekretsResponse {
        return StemmekretsResponse(
            id = stemmekrets.id,
            stemmekretsnavn = stemmekrets.stemmekretsnavn!!,
            stemmekretsnummer = stemmekrets.stemmekretsnummer!!,
            identifikasjon = Identifikasjon(stemmekrets.lokalid, stemmekrets.navnerom, stemmekrets.versjonid),
            kommunenummer = Kommunenummer(stemmekrets.kommune.kommunenummer, kommunenummer),
            tellekretsnummer = stemmekrets.tellekretsnummer,
            tellekretsnavn = stemmekrets.tellekretsnavn,
            valgdistriktsnummer = stemmekrets.valgdistriktsnummer,
            features = FeatureCollection(
                features = listOf(
                    posisjonToFeature(stemmekrets.posisjon, stemmekrets.version),
                    flateToFeature(stemmekrets, kodelisteService, stemmekrets.version)
                )
            ),
            version = version
        )
    }
}
