package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import java.time.LocalDate

interface Flate : CommonMetadataProperties {
    val id: String
    var omraade: MultiPolygon?
    var posisjon: Point?
    val type: Flatetype
    fun getGrensekoblinger(gyldighetsdato: LocalDate = LocalDate.now()): Set<GrenseKobling>

    fun getFlateInformasjon(): FlateInformasjon

    fun toEventRad(eventType: EventType): EventRad
}

interface KobletTilGrense {
    val lokalid: String
    val version: Int

    fun addGrensekobling(grense: Grense)
    fun removeGrensekobling(gammelGrense: Grense)
    fun replaceGrensekobling(gammelGrense: Grense, grense: Grense?)
    fun oppdaterOmraadeOgPosisjon()
}

data class FlateInformasjon (
    val flatetype: Flatetype,
    val lesbartNavn: String
)
