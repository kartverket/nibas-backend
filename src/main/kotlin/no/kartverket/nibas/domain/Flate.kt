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

    /** Kobler grensen til denne flaten, og flaten til grensen. Gir feil hvis flaten ikke kan ha grensetypen. */
    fun addGrensekobling(grense: Grense)
    fun removeGrensekobling(gammelGrense: Grense)

    /**
     * Bytter koblingen til [gammelGrense] med en kobling til [grense], og fjerner flaten fra [gammelGrense].
     * Gjør ingenting hvis flaten ikke er koblet til [gammelGrense]. Med `null` fjernes bare den gamle koblingen.
     */
    fun replaceGrensekobling(gammelGrense: Grense, grense: Grense?)
    fun oppdaterOmraadeOgPosisjon()
}

data class FlateInformasjon (
    val flatetype: Flatetype,
    val lesbartNavn: String
)
