package no.kartverket.nibas.common.geometry

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import org.locationtech.jts.operation.valid.TopologyValidationError

class UgyldigGeometriException(
    val geometry: Geometry,
    val topologyValidationError: TopologyValidationError
) : RuntimeException("Geometri-objekt er ikke gyldig: $geometry. Validation-feil: ${topologyValidationError.message}. Feil-lokasjon: ${topologyValidationError.coordinate}")

class IkkeSammenhengendePolygonException(
    sluttPunkt: Point,
    startPunkt: Point,
    topologyValidationError: TopologyValidationError
) : RuntimeException("Polygon som skal dannes har ikke-sammenhengende punkter, sluttpunkt: ${sluttPunkt.coordinate}, startPunkt: ${startPunkt.coordinate}. Validation-feil: ${topologyValidationError.message}.")
