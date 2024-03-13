package no.kartverket.nibas.common.geometry

import no.kartverket.nibas.domain.FlateInformasjon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.operation.valid.TopologyValidationError

class UgyldigGeometriException(
    val topologyValidationError: TopologyValidationError
) : RuntimeException("Geometri-objekt er ikke gyldig")

class IkkeSammenhengendePolygonException(
    sluttPunkt: Point,
    startPunkt: Point,
    topologyValidationError: TopologyValidationError
) : RuntimeException("Polygon som skal dannes har ikke-sammenhengende punkter, sluttpunkt: ${sluttPunkt.coordinate}, startPunkt: ${startPunkt.coordinate}. Validation-feil: ${topologyValidationError.message}.")

class UgyldigeFlaterException(
    val ugyldigeFlater: List<UgyldigFlateInformasjon> = emptyList()
) : RuntimeException("Feil ved flatedanning")

class UgyldigFlateInformasjon(
    val lokalid: String,
    val flateInformasjon: FlateInformasjon,
    val topologifeil: TopologyValidationError
)
