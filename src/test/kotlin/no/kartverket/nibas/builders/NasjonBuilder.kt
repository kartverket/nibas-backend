package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Spraakkode
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

class NasjonBuilder(private val id: String = "1") {

    private var navn: String = "Norge"
    private var spraak: String = "nor"
    private var lokalId: String = "123"
    private var fylke: Fylke = Fylke()

    companion object {
        val DEFAULT_NASJON = NasjonBuilder().build()
    }

    fun withNavn(navn: String): NasjonBuilder {
        this.navn = navn
        return this
    }

    fun build(): Nasjon {
        val nasjon = Nasjon(id = this.id, lokalid = lokalId)
        val spraakkode = Spraakkode()
        spraakkode.id = spraak
        spraakkode.description = spraak

        val administrativenhetnavn = Administrativenhetnavn()
        administrativenhetnavn.navn = navn
        administrativenhetnavn.spraak = spraakkode

        nasjon.administrativenhetnavn = mutableSetOf(administrativenhetnavn)
        nasjon.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        nasjon.flate = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))

        nasjon.addFylke(fylke)

        return nasjon
    }

    fun withFylke(fylke: Fylke): NasjonBuilder {
        this.fylke = fylke
        return this
    }

}
