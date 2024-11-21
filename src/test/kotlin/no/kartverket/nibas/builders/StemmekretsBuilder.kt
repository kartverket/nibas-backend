package no.kartverket.nibas.builders

import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate
import java.time.ZonedDateTime

class StemmekretsBuilder(
    private val id: String = "1",
    private var grenser: List<Grense> = emptyList(),
    private var kommune: Kommune? = KommuneBuilder.DEFAULT_KOMMUNE,
    private var lokalId: Lokalid = Lokalid(LOKALID),
    private var stemmekretsnavn: String = "Nordre Follo",
    private var stemmekretsnummer: String = "9",
    private var tellekretsnavn: String? = "Nordre Follo Tellekrets",
    private var tellekretsnummer: String? = "98",
    private var valgdistriktnummer: String = "19",
    private var endretdato: ZonedDateTime = ZonedDateTime.now(),
    private var datafangstdato: ZonedDateTime = ZonedDateTime.now(),
    private var gyldigFra: LocalDate = LocalDate.now(),
    private var gyldigTil: LocalDate? = null,
    private var informasjonstekst: String = INFORMASJONTEKST,
    private var opphav: String = OPPHAV,
    private var typeEndring: Endringstype = Endringstype.KVALITETSHEVING
) {

    companion object {
        val DEFAULT_STEMMEKRETS: Stemmekrets = StemmekretsBuilder().build()
    }

    fun build(): Stemmekrets {
        val stemmekrets = buildUtenKommuneOgGrenser()

        kommune?.addStemmekrets(stemmekrets)

        grenser.forEach { stemmekrets.addGrensekobling(it) }
        if (grenser.isNotEmpty() && grenser.none { it.grense.isEmpty }) {
            stemmekrets.oppdaterOmraadeOgPosisjon()
            if (!stemmekrets.omraade!!.isEmpty) {
                stemmekrets.posisjon = stemmekrets.omraade?.interiorPoint
            }
        }

        return stemmekrets
    }

    fun buildUtenKommuneOgGrenser(): Stemmekrets {
        val stemmekrets = Stemmekrets(id = this.id, lokalid = lokalId.value)

        stemmekrets.stemmekretsnavn = stemmekretsnavn
        stemmekrets.stemmekretsnummer = stemmekretsnummer
        stemmekrets.tellekretsnavn = tellekretsnavn
        stemmekrets.tellekretsnummer = tellekretsnummer
        stemmekrets.valgdistriktsnummer = valgdistriktnummer

        stemmekrets.auditInfo.endretdato = endretdato
        stemmekrets.auditInfo.typeEndring = typeEndring
        stemmekrets.gyldighet = Gyldighet(gyldigFra, gyldigTil)
        stemmekrets.setInfo(informasjonstekst)
        stemmekrets.opphav = opphav
        stemmekrets.datafangstdato = datafangstdato

        stemmekrets.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        stemmekrets.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))

        return stemmekrets
    }

    fun withLokalId(lokalId: String): StemmekretsBuilder {
        this.lokalId = Lokalid(lokalId)
        return this
    }

    fun withNavn(navn: String): StemmekretsBuilder {
        this.stemmekretsnavn = navn
        return this
    }

    fun withDatafangstdato(zonedDateTime: ZonedDateTime): StemmekretsBuilder {
        this.datafangstdato = zonedDateTime
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

    fun withGrenser(grenser: List<Grense>): StemmekretsBuilder {
        this.grenser = this.grenser + grenser
        return this
    }
}
