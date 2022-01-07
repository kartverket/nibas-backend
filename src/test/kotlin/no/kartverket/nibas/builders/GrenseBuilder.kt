package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Riksgrense
import org.locationtech.jts.geom.GeometryFactory

class GrenseBuilder(val id: Int = 1) {

    private var lokalId: String = "123"
    private var navnerom: String = "A.B.C"

    fun buildFylkesgrense(): Fylkesgrense {
        val grense = Fylkesgrense()
        grense.id = id
        grense.lokalid = lokalId
        grense.navnerom = navnerom
        grense.grense = GeometryFactory().createLineString()
        return grense
    }

    fun buildRiksgrense(): Riksgrense {
        val grense = Riksgrense()
        grense.id = id
        grense.lokalid = lokalId
        grense.navnerom = navnerom
        grense.grense = GeometryFactory().createLineString()
        return grense
    }

    fun buildKommunegrense(): Kommunegrense {
        val grense = Kommunegrense()
        grense.id = id
        grense.lokalid = lokalId
        grense.navnerom = navnerom
        grense.grense = GeometryFactory().createLineString()
        return grense
    }

    fun withLokalId(lokalId: String): GrenseBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withNavnerom(navnerom: String): GrenseBuilder {
        this.navnerom = navnerom
        return this
    }
}