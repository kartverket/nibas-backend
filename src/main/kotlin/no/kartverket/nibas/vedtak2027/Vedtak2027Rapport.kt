package no.kartverket.nibas.vedtak2027

import no.kartverket.nibas.events.EventRad

/**
 * Hva jobben gjorde, i den rekkefølgen den gjorde det. Logges av [Vedtak2027JobRunner].
 * [endredeFlater] er hendelsene som publiseres til nibas-events.
 */
class Vedtak2027Rapport {
    val linjer: MutableList<String> = mutableListOf()
    var endredeFlater: List<EventRad> = emptyList()

    fun logg(linje: String) {
        linjer.add(linje)
    }

    override fun toString(): String =
        (linjer + "Endrede flater:" + endredeFlater.map { "  ${it.flateType} ${it.lokalid}" }).joinToString("\n")
}
