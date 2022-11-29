package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate

class StemmekretsBuilder(private val id: String = "1") {

    private var grenser: MutableSet<Grense> = mutableSetOf()
    private var kommune: Kommune = KommuneBuilder.DEFAULT_KOMMUNE
    private var lokalId: String = LOKALID
    private var stemmekretsnavn = "Nordre Follo"
    private var stemmekretsnummer = "9"
    private var tellekretsnavn = "Nordre Follo Tellekrets"
    private var tellekretsnummer = "98"

    private var endretdato: LocalDate = LocalDate.now()
    private var datafangstdato: LocalDate = LocalDate.now()
    private var gyldigFra: LocalDate = LocalDate.now()
    private var gyldigTil: LocalDate? = null
    private var informasjonstekst = INFORMASJONTEKST
    private var opphav = OPPHAV

    companion object {
        val DEFAULT_STEMMEKRETS: Stemmekrets = StemmekretsBuilder().build()
    }

    fun build(): Stemmekrets {
        val stemmekrets = Stemmekrets(id = this.id, lokalid = lokalId)

        stemmekrets.datafangstdato = datafangstdato

        stemmekrets.stemmekretsnavn = stemmekretsnavn
        stemmekrets.stemmekretsnummer = stemmekretsnummer
        stemmekrets.tellekretsnavn = tellekretsnavn
        stemmekrets.tellekretsnummer = tellekretsnummer

        stemmekrets.auditInfo.endretdato = endretdato

        stemmekrets.kommune = kommune

        stemmekrets.auditInfo.endretdato = endretdato
        stemmekrets.gyldighet = Gyldighet(gyldigFra, gyldigTil)
        stemmekrets.setInfo(informasjonstekst)
        stemmekrets.opphav = opphav

        stemmekrets.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        stemmekrets.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))
        grenser.forEach {
            stemmekrets.addGrensekobling(it, GrenseKoblingAttributter())
        }

        return stemmekrets
    }

    fun withLokalId(lokalId: String): StemmekretsBuilder {
        this.lokalId = lokalId
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

    fun withGrenser(vararg grenser: Grense): StemmekretsBuilder {
        this.grenser = grenser.toMutableSet()
        return this
    }
}
