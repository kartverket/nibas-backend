package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate

class GrunnkretsBuilder(
    private val id: String = "1",
    private var lokalId: Lokalid = Lokalid(LOKALID),
    private var grunnkretsnummer: String = "12",
    private var grunnkretsnavn: String = "Dåsvatn",
    private var grenser: MutableSet<Grense> = mutableSetOf(),
    private var kommune: Kommune = KommuneBuilder.DEFAULT_KOMMUNE,
    private var endretdato: LocalDate = LocalDate.now(),
    private var datafangstdato: LocalDate = LocalDate.now(),
    private var gyldigFra: LocalDate = LocalDate.now(),
    private var gyldigTil: LocalDate? = null,
    private var slettet: Boolean = false,
    private var informasjonstekst: String = INFORMASJONTEKST,
    private var opphav: String = OPPHAV,
    private var typeEndring: Endringstype = Endringstype.KVALITETSHEVING
) {

    companion object {
        val DEFAULT_GRUNNKRETS: Grunnkrets = GrunnkretsBuilder().build()
    }

    fun build(): Grunnkrets {
        val grunnkrets = Grunnkrets(id = this.id, lokalid = lokalId.value)

        grunnkrets.datafangstdato = datafangstdato

        grunnkrets.grunnkretsnavn = grunnkretsnavn
        grunnkrets.grunnkretsnummer = grunnkretsnummer
        grunnkrets.auditInfo.endretdato = endretdato
        grunnkrets.kommune = kommune

        grunnkrets.auditInfo.endretdato = endretdato
        grunnkrets.gyldighet = Gyldighet(gyldigFra, gyldigTil, slettet)
        grunnkrets.setInfo(informasjonstekst)
        grunnkrets.opphav = opphav
        grunnkrets.auditInfo.typeEndring = typeEndring

        grunnkrets.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        grunnkrets.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))
        grenser.forEach {
            grunnkrets.addGrensekobling(it, GrenseKoblingAttributter())
        }

        return grunnkrets
    }

    fun withLokalId(lokalId: String): GrunnkretsBuilder {
        this.lokalId = Lokalid((lokalId))
        return this
    }

    fun withLokalId(lokalId: Lokalid): GrunnkretsBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withEndretDato(dateTime: LocalDate): GrunnkretsBuilder {
        this.endretdato = dateTime
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

    fun withGyldigTil(dateTime: LocalDate?): GrunnkretsBuilder {
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
                grenser.add(Grense(grensetype = pair.first))
            }
        }
        this.grenser = grenser.toMutableSet()
        return this
    }

    fun withGyldighetsIntervall(pair: Pair<LocalDate, LocalDate?>): GrunnkretsBuilder {
        this.withGyldigFra(pair.first)
            .withGyldigTil(pair.second)
        return this
    }

}
