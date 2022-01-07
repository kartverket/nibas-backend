package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.*
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

class NasjonBuilder(private val id: Int = 1) {

    private var navn: String = "Norge"
    private var spraak: String = "nor"
    private var navnerom: String = "ABC"
    private var lokalId: String = "123"
    private var avtaltavgrensningslinjer: MutableSet<Avtaltavgrensningslinje> = mutableSetOf()
    private var territorialgrenser: MutableSet<Territorialgrense> = mutableSetOf()
    private var riksgrenser: MutableSet<Riksgrense> = mutableSetOf()

    companion object {
        val DEFAULT_NASJON = NasjonBuilder().build()
    }

    fun withNavn(navn: String): NasjonBuilder {
        this.navn = navn
        return this
    }

    fun build(): Nasjon {
        val nasjon = Nasjon(id = this.id)
        val spraakkode = Spraakkode()
        spraakkode.id = spraak
        spraakkode.description = spraak

        val administrativenhetnavn = Administrativenhetnavn()
        administrativenhetnavn.navn = navn
        administrativenhetnavn.spraak = spraakkode

        nasjon.administrativenhetnavn = mutableSetOf(administrativenhetnavn)
        nasjon.lokalid = lokalId
        nasjon.navnerom = navnerom
        nasjon.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        nasjon.flate = GeometryFactory().createPolygon()

        nasjon.riksgrenser = riksgrenser
        nasjon.territorialgrenser = territorialgrenser
        nasjon.avtaltavgrensningslinjer = avtaltavgrensningslinjer
        return nasjon
    }

    fun withRiksgrenser(vararg riksgrenser: Riksgrense): NasjonBuilder {
        this.riksgrenser = riksgrenser.toMutableSet()
        return this
    }

    fun withTerritorialgrenser(vararg territorialgrenser: Territorialgrense): NasjonBuilder {
        this.territorialgrenser = territorialgrenser.toMutableSet()
        return this
    }

    fun withAvtaltavgrensningslinjer(vararg avtaltavgrensningslinjer: Avtaltavgrensningslinje): NasjonBuilder {
        this.avtaltavgrensningslinjer = avtaltavgrensningslinjer.toMutableSet()
        return this
    }


}