package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString

class GrenseBuilder(private val id: String = "1") {

    private var lokalId: String = "123"
    private var navnerom: String = "A.B.C"
    private var grenseGeometri: LineString = GeometryFactory().createLineString()

    fun buildFylkesgrense(): Grense {
        return FylkesgrenseBuilder(id).build()
    }

    fun buildRiksgrense(): Grense {
        val grense = Grense(Grensetype.Riksgrense, id = this.id)
        grense.lokalid = lokalId
        grense.navnerom = navnerom
        grense.grense = grenseGeometri
        return grense
    }

    fun buildKommunegrense(): Grense {
        val grense = Grense(Grensetype.Kommunegrense, id = this.id)
        grense.lokalid = lokalId
        grense.navnerom = navnerom
        grense.grense = grenseGeometri
        return grense
    }

    fun buildGrunnkretsgrense(): Grense {
        val grense = Grense(Grensetype.Grunnkretsgrense, id = this.id)
        grense.lokalid = lokalId
        grense.navnerom = navnerom
        grense.grense = grenseGeometri
        return grense
    }

    fun withLokalId(lokalId: String): GrenseBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withGrenseGeometri(coordinates: List<Coordinate>): GrenseBuilder {
        this.grenseGeometri = GeometryFactory().createLineString(coordinates.toTypedArray())
        return this
    }

}
