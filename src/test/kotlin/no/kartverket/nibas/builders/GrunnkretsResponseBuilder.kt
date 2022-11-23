package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.GyldighetResponse
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.inndelinger.flateToFeature
import no.kartverket.nibas.inndelinger.posisjonToFeature
import no.kartverket.nibas.kodelister.KodelisteService
import java.time.LocalDate
import java.time.Month

class GrunnkretsResponseBuilder(
    private val kodelisteService: KodelisteService,
    private var grunnkrets: Grunnkrets = GrunnkretsBuilder().build(),
    private var kommunenummer: String = "1787",
    private var version: Int = 1,
    private var gyldighetResponse: GyldighetResponse = GyldighetResponse(LocalDate.of(2022, Month.JANUARY, 1), null),
    private var oppdateringsdato: LocalDate = LocalDate.of(2022, Month.AUGUST, 1),
    private var endringstype: String = "Kvalitetsheving"
) {

    fun build(): GrunnkretsResponse {
        val lokalid = grunnkrets.lokalid
        return GrunnkretsResponse(
            id = lokalid,
            identifikasjon = Identifikasjon(lokalid),
            navn = grunnkrets.grunnkretsnavn!!,
            grunnkretsnummer = grunnkrets.grunnkretsnummer!!,
            gyldighet = gyldighetResponse,
            oppdateringsdato = oppdateringsdato,
            endringstype =  endringstype,
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
