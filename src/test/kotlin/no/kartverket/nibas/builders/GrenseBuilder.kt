package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Riksgrense
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString

class GrenseBuilder(private val id: String = "1") {

    private var lokalId: String = "123"
    private var navnerom: String = "A.B.C"
    private var grenseGeometri: LineString = GeometryFactory().createLineString()

    fun buildFylkesgrense(): Fylkesgrense {
        val grense = Fylkesgrense(id = this.id)
        grense.lokalid = lokalId
        grense.navnerom = navnerom
        grense.grense = grenseGeometri
        return grense
    }

    fun buildRiksgrense(): Riksgrense {
        val grense = Riksgrense(id = this.id)
        grense.lokalid = lokalId
        grense.navnerom = navnerom
        grense.grense = grenseGeometri
        return grense
    }

    fun buildKommunegrense(): Kommunegrense {
        val grense = Kommunegrense(id = this.id)
        grense.lokalid = lokalId
        grense.navnerom = navnerom
        grense.grense = grenseGeometri
        return grense
    }

    fun withLokalId(lokalId: String): GrenseBuilder {
        this.lokalId = lokalId
        return this
    }

}
