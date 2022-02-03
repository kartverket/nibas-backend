package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Territorialgrense
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate

class FylkeBuilder(private val id: String = "1") {
    private var avtaltavgrensningslinjer: MutableSet<Avtaltavgrensningslinje> = mutableSetOf()
    private var objtype: String = "Fylke"
    private var territorialgrenser: MutableSet<Territorialgrense> = mutableSetOf()
    private var fylkesgrenser: MutableSet<Fylkesgrense> = mutableSetOf()
    private var riksgrenser: MutableSet<Riksgrense> = mutableSetOf()
    private var fylkesnummer: String = "UUID-1"
    private var navnerom: String = "ABC"
    private var lokalId: String = "123"
    private var navn = "Vestland"
    private var spraak = "NOR"
    private var samiskforvaltningsomraade = false
    private var oppdateringsdato = LocalDate.now()

    companion object {
        val DEFAULT_FYLKE = FylkeBuilder().build()
    }

    fun withNavn(navn: String, spraak: String = "NOR"): FylkeBuilder {
        this.navn = navn
        this.spraak = spraak
        return this
    }

    fun build(): Fylke {
        val fylke = Fylke(id = this.id)
        val spraakkode = Spraakkode()
        spraakkode.id = spraak
        spraakkode.description = spraak

        val administrativenhetnavn = Administrativenhetnavn()
        administrativenhetnavn.navn = navn
        administrativenhetnavn.spraak = spraakkode

        fylke.administrativenhetnavn = mutableSetOf(administrativenhetnavn)
        administrativenhetnavn.fylkeFk = fylke

        fylke.objtype = objtype
        fylke.lokalid = lokalId
        fylke.navnerom = navnerom
        fylke.fylkesnummer = fylkesnummer
        fylke.samiskforvaltningsomraade = samiskforvaltningsomraade
        fylke.oppdateringsdato = oppdateringsdato
        fylke.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        fylke.flate = GeometryFactory().createPolygon()

        fylke.fylkesgrenser = fylkesgrenser
        fylke.riksgrenser = riksgrenser
        fylke.territorialgrenser = territorialgrenser
        fylke.avtaltavgrensningslinjer = avtaltavgrensningslinjer

        return fylke
    }

    fun withLokalId(lokalId: String): FylkeBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withNavnerom(navnerom: String): FylkeBuilder {
        this.navnerom = navnerom
        return this
    }

    fun withFylkesnummer(fylkesnummer: String): FylkeBuilder {
        this.fylkesnummer = fylkesnummer
        return this
    }

    fun withRiksgrenser(vararg riksgrenser: Riksgrense): FylkeBuilder {
        this.riksgrenser = riksgrenser.toMutableSet()
        return this
    }

    fun withFylkesgrenser(vararg fylkesgrenser: Fylkesgrense): FylkeBuilder {
        this.fylkesgrenser = fylkesgrenser.toMutableSet()
        return this
    }

    fun withTerritorialgrenser(vararg territorialgrenser: Territorialgrense): FylkeBuilder {
        this.territorialgrenser = territorialgrenser.toMutableSet()
        return this
    }

    fun withAvtaltavgrensningslinjer(vararg avtaltavgrensningslinjer: Avtaltavgrensningslinje): FylkeBuilder {
        this.avtaltavgrensningslinjer = avtaltavgrensningslinjer.toMutableSet()
        return this
    }
}
