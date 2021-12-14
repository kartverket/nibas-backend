package no.kartverket.nibas.controller

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.toAdministrativEnhetResponse
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.service.AdministrativEnhetService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import no.kartverket.nibas.api.v1.FeatureApi as FeatureApiV1

@RestController
class FeatureKontroller : FeatureApiV1 {

    @Autowired
    lateinit var administrativenhetService: AdministrativEnhetService

    override fun hentAdministrativeEnheter(type: AdministrativEnhetType, ider: List<Int>): FeatureCollection {
        val featureCollection = FeatureCollection()
        val administrativeEnheter = administrativenhetService.hentAdministrativeEnheterMedId(ider, type)
        if (administrativeEnheter.any()) {
            administrativeEnheter.forEach { administrativEnhet ->

                val grenser = when(administrativEnhet) {
                    is Kommune -> administrativEnhet.kommunegrenser
                    is Fylke -> administrativEnhet.fylkesgrenser
                    is Nasjon -> administrativEnhet.riksgrenser
                    else -> throw RuntimeException("Ukjent grense")
                }

                grenser.forEach { grense ->
                    featureCollection.add(
                        Feature(
                            properties = FeatureProperties(
                                grense.id!!,
                                grense.objtype!!,
                                type,
                                toAdministrativEnhetResponse(administrativEnhet, type)
                            ),
                            geometry = grense.grense!!
                        )
                    )
                }
            }
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return featureCollection
    }

    override fun oppdaterGrenser(
        type: AdministrativEnhetType,
        administratitveEnheter: FeatureCollection
    ) {
        administrativenhetService.oppdaterGrenser(type, administratitveEnheter.features)
    }

}