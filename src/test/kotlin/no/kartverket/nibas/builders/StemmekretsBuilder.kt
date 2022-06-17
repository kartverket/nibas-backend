package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Stemmekrets
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate

class StemmekretsBuilder(private val id: String = "1") {

    private var grenser: MutableSet<Grense> = mutableSetOf()
    private var kommunenummer: String = "UUID-1"
    private var lokalId: String = LOKALID
    private var versjonId: String = VERSJON_33
    private var navnerom: String = NAVNEROM
    private var stemmekretsnavn = "Nordre Follo"
    private var stemmekretsnummer = "9"
    private var tellekretsnavn = "Nordre Follo Tellekrets"
    private var tellekretsnummer = "98"


    private var oppdateringsdato: LocalDate = LocalDate.now()
    private var datafangstdato: LocalDate = LocalDate.now()
    private var gyldigFra: LocalDate = LocalDate.now()
    private var gyldigTil: LocalDate = LocalDate.now()
    private var informasjonstekst = INFORMASJONTEKST
    private var opphav = OPPHAV

    companion object {
        val DEFAULT_STEMMEKRETS: Stemmekrets = StemmekretsBuilder().build()
    }

    fun build(): Stemmekrets {
        val stemmekrets = Stemmekrets(id = this.id)

        stemmekrets.datafangstdato = datafangstdato

        stemmekrets.lokalid = lokalId
        stemmekrets.versjonid = versjonId
        stemmekrets.navnerom = navnerom

        stemmekrets.stemmekretsnavn = stemmekretsnavn
        stemmekrets.stemmekretsnummer = stemmekretsnummer
        stemmekrets.tellekretsnavn = tellekretsnavn
        stemmekrets.tellekretsnummer = tellekretsnummer

        stemmekrets.kommunenummer = kommunenummer
        stemmekrets.auditInfo.oppdateringsdato = oppdateringsdato

        stemmekrets.auditInfo.oppdateringsdato = oppdateringsdato
        stemmekrets.gyldigFra = gyldigFra
        stemmekrets.gyldigTil = gyldigTil
        stemmekrets.setInfo(informasjonstekst)
        stemmekrets.opphav = opphav

        stemmekrets.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        stemmekrets.flate = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))
        grenser.forEach {
            stemmekrets.addGrensekobling(it, GrenseKoblingAttributter())
        }

        return stemmekrets
    }

    fun withLokalId(lokalId: String): StemmekretsBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withNavnerom(navnerom: String): StemmekretsBuilder {
        this.navnerom = navnerom
        return this
    }

    fun withOppdateringsdato(dateTime: LocalDate): StemmekretsBuilder {
        this.oppdateringsdato = dateTime
        return this
    }

    fun withDatafangstdato(dateTime: LocalDate): StemmekretsBuilder {
        this.datafangstdato = dateTime
        return this
    }

    fun withGyldigFra(dateTime: LocalDate): StemmekretsBuilder {
        this.gyldigFra = dateTime
        return this
    }

    fun withGyldigTil(dateTime: LocalDate): StemmekretsBuilder {
        this.gyldigTil = dateTime
        return this
    }

    fun withGrenser(vararg grenser: Grense): StemmekretsBuilder {
        this.grenser = grenser.toMutableSet()
        return this
    }

    fun withRandomGrenser(vararg grensePairs: Pair<Grensetype, Int>): StemmekretsBuilder {
        val grenser = mutableListOf<Grense>()
        grensePairs.forEach { pair ->
            repeat(pair.second) {
                grenser.add(Grense(pair.first))
            }
        }
        this.grenser = grenser.toMutableSet()
        return this
    }

}
