package no.kartverket.nibas.common.geometry

import no.kartverket.nibas.domain.FlateInformasjon
import org.locationtech.jts.geom.Coordinate
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
) : RuntimeException("Feil ved flatedanning: ${ugyldigeFlater.joinToString(", ") { "[${it.lokalid} - ${it.topologifeil}]" }}")

class UgyldigFlateInformasjon(
    val lokalid: String,
    val flateInformasjon: FlateInformasjon,
    val topologifeil: String,
    val koordinat: Coordinate
)

fun TopologyValidationError.toCustomText(): String =
    topologyValidationErrorToCustomText(this.errorType)

fun topologyValidationErrorToCustomText(errorCode: Int): String  {
    return when(errorCode) {
        TopologyValidationError.SELF_INTERSECTION -> "En grense kan ikke krysse en annen grense"
        TopologyValidationError.RING_SELF_INTERSECTION -> "En grense kan ikke krysse en annen grense"
        TopologyValidationError.INVALID_COORDINATE -> "Grensen inneholder ett eller flere punkter med ugyldig koordinat"
        TopologyValidationError.TOO_FEW_POINTS -> "For få grensepunkter i flaten"
        TopologyValidationError.RING_NOT_CLOSED -> "Flaten er ikke lukket"
        else -> "Ukjent feil med feilkode [$errorCode: ${TopologyValidationError(errorCode).message}]"
    }
}


class IngenGrenserForFlateException: RuntimeException("Feil ved flatedanning: Flaten har ingen grenser.")
