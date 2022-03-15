package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.KommuneAvtaltavgrensningslinje
import no.kartverket.nibas.domain.KommuneFylkesgrense
import no.kartverket.nibas.domain.KommuneKommunegrense
import no.kartverket.nibas.domain.KommuneRiksgrense
import no.kartverket.nibas.domain.KommuneTerritorialgrense
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Territorialgrense
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.ZonedDateTime

class KommuneBuilder(private val id: String = "1") {
    private var avtaltavgrensningslinjer: MutableSet<Avtaltavgrensningslinje> = mutableSetOf()
    private var territorialgrenser: MutableSet<Territorialgrense> = mutableSetOf()
    private var fylkesgrenser: MutableSet<Fylkesgrense> = mutableSetOf()
    private var kommunegrenser: MutableSet<Kommunegrense> = mutableSetOf()
    private var riksgrenser: MutableSet<Riksgrense> = mutableSetOf()
    private var kommunenummer: String = "UUID-1"
    private var navnerom: String = "ABC"
    private var lokalId: String = "123"
    private var navn = "Vestland"
    private var spraak = "NOR"
    private var samiskforvaltningsomraade = false
    private var oppdateringsdato: ZonedDateTime = ZonedDateTime.now()

    companion object {
        val DEFAULT_KOMMUNE = KommuneBuilder().build()
    }

    fun withNavn(navn: String, spraak: String = "NOR"): KommuneBuilder {
        this.navn = navn
        this.spraak = spraak
        return this
    }

    fun build(): Kommune {
        val kommune = Kommune(id = this.id)
        val spraakkode = Spraakkode()
        spraakkode.id = spraak
        spraakkode.description = spraak

        val administrativenhetnavn = Administrativenhetnavn()
        administrativenhetnavn.navn = navn
        administrativenhetnavn.spraak = spraakkode

        kommune.administrativenhetnavn = mutableSetOf(administrativenhetnavn)
        administrativenhetnavn.kommuneFk = kommune

        kommune.lokalid = lokalId
        kommune.navnerom = navnerom
        kommune.kommunenummer = kommunenummer
        kommune.samiskforvaltningsomraade = samiskforvaltningsomraade
        kommune.oppdateringsdato = oppdateringsdato
        kommune.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        kommune.flate = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))

        kommune.kommunegrenser = kommunegrenser.map { KommuneKommunegrense(kommune, it, GrenseKoblingAttributter()) }.toMutableSet()
        kommune.fylkesgrenser = fylkesgrenser.map { KommuneFylkesgrense(kommune, it, GrenseKoblingAttributter()) }.toMutableSet()
        kommune.riksgrenser = riksgrenser.map { KommuneRiksgrense(kommune, it, GrenseKoblingAttributter()) }.toMutableSet()
        kommune.territorialgrenser = territorialgrenser.map { KommuneTerritorialgrense(kommune, it, GrenseKoblingAttributter()) }.toMutableSet()
        kommune.avtaltavgrensningslinjer = avtaltavgrensningslinjer.map { KommuneAvtaltavgrensningslinje(kommune, it, GrenseKoblingAttributter()) }.toMutableSet()

        return kommune
    }

    fun withLokalId(lokalId: String): KommuneBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withNavnerom(navnerom: String): KommuneBuilder {
        this.navnerom = navnerom
        return this
    }

    fun withKommunegrenser(vararg kommunegrenser: Kommunegrense): KommuneBuilder {
        this.kommunegrenser = kommunegrenser.toMutableSet()
        return this
    }

    fun withRiksgrenser(vararg riksgrenser: Riksgrense): KommuneBuilder {
        this.riksgrenser = riksgrenser.toMutableSet()
        return this
    }

    fun withFylkesgrenser(vararg fylkesgrenser: Fylkesgrense): KommuneBuilder {
        this.fylkesgrenser = fylkesgrenser.toMutableSet()
        return this
    }

    fun withTerritorialgrenser(vararg territorialgrenser: Territorialgrense): KommuneBuilder {
        this.territorialgrenser = territorialgrenser.toMutableSet()
        return this
    }

    fun withAvtaltavgrensningslinjer(vararg avtaltavgrensningslinjer: Avtaltavgrensningslinje): KommuneBuilder {
        this.avtaltavgrensningslinjer = avtaltavgrensningslinjer.toMutableSet()
        return this
    }
}
