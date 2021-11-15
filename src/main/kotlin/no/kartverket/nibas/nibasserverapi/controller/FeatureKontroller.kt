package no.kartverket.nibas.nibasserverapi.controller

import no.kartverket.nibas.nibasserverapi.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.nibasserverapi.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.nibasserverapi.api.v1.common.geojson.Feature
import no.kartverket.nibas.nibasserverapi.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.nibasserverapi.api.v1.toAdministrativEnhetResponse
import no.kartverket.nibas.nibasserverapi.service.AdministrativEnhetService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import no.kartverket.nibas.nibasserverapi.api.v1.FeatureApi as FeatureApiV1

@RestController
class FeatureKontroller : FeatureApiV1 {

    @Autowired
    lateinit var administrativenhetService: AdministrativEnhetService

    override fun hentAdministrativeEnheter(type: AdministrativEnhetType, ider: List<Int>): FeatureCollection {
        return createFeatureCollection(ider, type)
    }

    override fun oppdaterGrenser(
        type: AdministrativEnhetType,
        ider: List<Int>,
        administratitveEnheter: FeatureCollection
    ): FeatureCollection {
        administrativenhetService.oppdaterGrenser(type, administratitveEnheter.features)
        return createFeatureCollection(ider, type)
    }

    private fun createFeatureCollection(
        ider: List<Int>,
        type: AdministrativEnhetType
    ): FeatureCollection {
        val featureCollection = FeatureCollection()

        val administrativeEnheter = administrativenhetService.hentAdministrativeEnheterMedId(ider, type)
        if (administrativeEnheter.any()) {
            administrativeEnheter.forEach { administrativEnhet ->
                administrativEnhet.grenser.forEach { grense ->
                    featureCollection.add(
                        Feature(
                            properties = FeatureProperties(
                                grense.id,
                                grense.navn,
                                type,
                                toAdministrativEnhetResponse(administrativEnhet, type)
                            ),
                            geometry = grense.kurve
                        )
                    )
                }
            }
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return featureCollection
    }

}