package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.utils.GrenseHelper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate
import java.time.ZonedDateTime

class GrunnkretsBuilder(
    private val id: String = "1",
    private var lokalId: Lokalid = Lokalid(LOKALID),
    private var grunnkretsnummer: String = "12",
    private var grunnkretsnavn: String = "Dåsvatn",
    private var grenser: List<Grense> = emptyList(),
    private var kommune: Kommune? = KommuneBuilder.DEFAULT_KOMMUNE,
    private var endretdato: ZonedDateTime = ZonedDateTime.now(),
    private var datafangstdato: ZonedDateTime = ZonedDateTime.now(),
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
        val grunnkrets = buildUtenKommune()

        kommune?.addGrunnkrets(grunnkrets)

        return grunnkrets
    }

    fun buildUtenKommune(): Grunnkrets {
        val grunnkrets = Grunnkrets(id = this.id, lokalid = lokalId.value)

        grunnkrets.datafangstdato = datafangstdato

        grunnkrets.grunnkretsnavn = grunnkretsnavn
        grunnkrets.grunnkretsnummer = grunnkretsnummer
        grunnkrets.gyldighet = Gyldighet(gyldigFra, gyldigTil, slettet)
        grunnkrets.setInfo(informasjonstekst)
        grunnkrets.opphav = opphav
        grunnkrets.auditInfo.endretdato = endretdato
        grunnkrets.auditInfo.typeEndring = typeEndring

        var forrigeGrense = grenser.lastOrNull()
        grenser.forEachIndexed { index, grense ->
            grunnkrets.addGrensekobling(grense, GrenseKoblingAttributter(
                rekkefoelge = index,
                retningMedKlokken = GrenseHelper.getRetningMedKlokken(grense, forrigeGrense)
            ))
            forrigeGrense = grense
        }
        grunnkrets.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        grunnkrets.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))

        //Hack fordi noen tester lager grenser uten linestring
        try {
            if (grenser.isNotEmpty()) {
                grunnkrets.oppdaterOmraadeOgPosisjon()
                if (!grunnkrets.omraade!!.isEmpty) {
                    grunnkrets.posisjon = grunnkrets.omraade?.interiorPoint
                }
            }
        } catch (_: UninitializedPropertyAccessException) {
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

    fun withNavn(navn: String): GrunnkretsBuilder {
        this.grunnkretsnavn = navn
        return this
    }

    fun withEndretDato(zonedDateTime: ZonedDateTime): GrunnkretsBuilder {
        this.endretdato = zonedDateTime
        return this
    }

    fun withDatafangstdato(zonedDateTime: ZonedDateTime): GrunnkretsBuilder {
        this.datafangstdato = zonedDateTime
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
        this.grenser = this.grenser.plus(grenser.toList())
        return this
    }

    fun withRandomGrenser(vararg grensePairs: Pair<Grensetype, Int>): GrunnkretsBuilder {
        val grenser = mutableListOf<Grense>()
        grensePairs.forEach { pair ->
            repeat(pair.second) {
                grenser.add(Grense(grensetype = pair.first))
            }
        }
        this.grenser = this.grenser.plus(grenser.toList())
        return this
    }

    fun withGyldighetsIntervall(pair: Pair<LocalDate, LocalDate?>): GrunnkretsBuilder {
        this.withGyldigFra(pair.first)
            .withGyldigTil(pair.second)
        return this
    }

}
