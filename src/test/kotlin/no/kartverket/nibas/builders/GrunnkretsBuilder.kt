package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate

class GrunnkretsBuilder(private val id: String = "1") {

    private var grenser: MutableSet<Grense> = mutableSetOf()
    private var kommunenummer: String = "UUID-1"
    private var lokalId: String = LOKALID
    private var versjonId: String = VERSJON_33
    private var navnerom: String = NAVNEROM
    private var grunnkretsnavn = "Dåsvatn"
    private var grunnkretsnummer = "12"

    private var oppdateringsdato: LocalDate = LocalDate.now()
    private var datafangstdato: LocalDate = LocalDate.now()
    private var gyldigFra: LocalDate = LocalDate.now()
    private var gyldigTil: LocalDate = LocalDate.now()
    private var informasjonstekst = INFORMASJONTEKST
    private var opphav = OPPHAV

    companion object {
        val DEFAULT_GRUNNKRETS: Grunnkrets = GrunnkretsBuilder().build()
    }

    fun build(): Grunnkrets {
        val grunnkrets = Grunnkrets(id = this.id)

        grunnkrets.datafangstdato = datafangstdato

        grunnkrets.lokalid = lokalId
        grunnkrets.versjonid = versjonId
        grunnkrets.navnerom = navnerom
        grunnkrets.grunnkretsnavn = grunnkretsnavn
        grunnkrets.grunnkretsnummer = grunnkretsnummer
        grunnkrets.kommunenummer = kommunenummer
        grunnkrets.auditInfo.oppdateringsdato = oppdateringsdato

        grunnkrets.auditInfo.oppdateringsdato = oppdateringsdato
        grunnkrets.gyldigFra = gyldigFra
        grunnkrets.gyldigTil = gyldigTil
        grunnkrets.setInfo(informasjonstekst)
        grunnkrets.opphav = opphav

        grunnkrets.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        grunnkrets.flate = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))
        grenser.forEach {
            grunnkrets.addGrensekobling(it, GrenseKoblingAttributter())
        }

        return grunnkrets
    }

    fun withLokalId(lokalId: String): GrunnkretsBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withNavnerom(navnerom: String): GrunnkretsBuilder {
        this.navnerom = navnerom
        return this
    }

    fun withOppdateringsdato(dateTime: LocalDate): GrunnkretsBuilder {
        this.oppdateringsdato = dateTime
        return this
    }

    fun withDatafangstdato(dateTime: LocalDate): GrunnkretsBuilder {
        this.datafangstdato = dateTime
        return this
    }

    fun withGyldigFra(dateTime: LocalDate): GrunnkretsBuilder {
        this.gyldigFra = dateTime
        return this
    }

    fun withGyldigTil(dateTime: LocalDate): GrunnkretsBuilder {
        this.gyldigTil = dateTime
        return this
    }

    fun withGrenser(vararg grenser: Grense): GrunnkretsBuilder {
        this.grenser = grenser.toMutableSet()
        return this
    }

    fun withRandomGrenser(vararg grensePairs: Pair<Grensetype, Int>): GrunnkretsBuilder {
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
