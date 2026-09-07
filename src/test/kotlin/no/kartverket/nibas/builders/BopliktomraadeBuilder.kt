package no.kartverket.nibas.builders

import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.BopliktomraadeGrenser
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate
import java.time.ZonedDateTime

class BopliktomraadeBuilder(
    private val id: String = "1",
    private var grenser: List<Grense> = emptyList(),
    private var kommune: Kommune? = KommuneBuilder.DEFAULT_KOMMUNE,
    private var lokalId: Lokalid = Lokalid(LOKALID),
    private var bopliktomraadenavn: String = "Bopliktområde i kommune",
    private var bopliktomraadenummer: String = "01",
    private var forskriftsreferanse: String? = "Forskrift om nedsatt konsesjonsgrense etter konsesjonsloven § 7",
    private var url: String? = "https://frogn.kommunegis.no/publikum",
    private var gjelderForBoligIkkeTattIBruk: Boolean = false,
    private var gjelderForBruktSomHelarsbolig: Boolean = false,
    private var gjelderForUbebygdBoligtomt: Boolean = false,
    private var harUnntakFraSlektskapsunntak: Boolean = false,
    private var andreLokaleAvgrensninger: String? = null,
    private var endretdato: ZonedDateTime = ZonedDateTime.now(),
    private var datafangstdato: ZonedDateTime = ZonedDateTime.now(),
    private var gyldigFra: LocalDate = LocalDate.now(),
    private var gyldigTil: LocalDate? = null,
    private var harUsikkerAvgrensning: Boolean = true,
    private var opphav: String = OPPHAV,
    private var typeEndring: Endringstype = Endringstype.KVALITETSHEVING
) {

    companion object {
        val DEFAULT_BOPLIKTOMRAADE: Bopliktomraade = BopliktomraadeBuilder().build()
    }

    fun build(): Bopliktomraade {
        val bopliktomraade = buildUtenKommuneOgGrenser()

        kommune?.addBopliktomraade(bopliktomraade)

        grenser.forEach { bopliktomraade.addGrensekobling(it) }
        if (grenser.isNotEmpty() && grenser.none { it.grense.isEmpty }) {
            bopliktomraade.oppdaterOmraadeOgPosisjon()
            if (!bopliktomraade.omraade!!.isEmpty) {
                bopliktomraade.posisjon = bopliktomraade.omraade?.interiorPoint
            }
        }

        return bopliktomraade
    }

    fun buildUtenKommuneOgGrenser(): Bopliktomraade {
        val bopliktomraade = Bopliktomraade(id = this.id, lokalid = lokalId.value)

        bopliktomraade.navn = bopliktomraadenavn
        bopliktomraade.nummer = bopliktomraadenummer
        bopliktomraade.forskriftsreferanse = forskriftsreferanse
        bopliktomraade.url = url
        bopliktomraade.bopliktomraadeGrenser = BopliktomraadeGrenser.DelvisBoplikt()
        bopliktomraade.gjelderForBoligIkkeTattIBruk = gjelderForBoligIkkeTattIBruk
        bopliktomraade.gjelderForBruktSomHelarsbolig = gjelderForBruktSomHelarsbolig
        bopliktomraade.gjelderForUbebygdBoligtomt = gjelderForUbebygdBoligtomt
        bopliktomraade.harUnntakFraSlektskapsunntak = harUnntakFraSlektskapsunntak
        bopliktomraade.andreLokaleAvgrensninger = andreLokaleAvgrensninger
        bopliktomraade.harUsikkerAvgrensning = harUsikkerAvgrensning

        bopliktomraade.auditInfo.endretdato = endretdato
        bopliktomraade.auditInfo.typeEndring = typeEndring
        bopliktomraade.gyldighet = Gyldighet(gyldigFra, gyldigTil)
        bopliktomraade.opphav = opphav
        bopliktomraade.datafangstdato = datafangstdato

        bopliktomraade.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        bopliktomraade.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))

        return bopliktomraade
    }

    fun withLokalId(lokalId: String): BopliktomraadeBuilder {
        this.lokalId = Lokalid(lokalId)
        return this
    }

    fun withNavn(navn: String): BopliktomraadeBuilder {
        this.bopliktomraadenavn = navn
        return this
    }

    fun withDatafangstdato(zonedDateTime: ZonedDateTime): BopliktomraadeBuilder {
        this.datafangstdato = zonedDateTime
        return this
    }

    fun withGyldigFra(dateTime: LocalDate): BopliktomraadeBuilder {
        this.gyldigFra = dateTime
        return this
    }

    fun withGyldigTil(dateTime: LocalDate): BopliktomraadeBuilder {
        this.gyldigTil = dateTime
        return this
    }

    fun withGrenser(grenser: List<Grense>): BopliktomraadeBuilder {
        this.grenser = this.grenser + grenser
        return this
    }

    fun withGjelderForBoligIkkeTattIBruk(gjelderForBoligIkkeTattIBruk: Boolean): BopliktomraadeBuilder {
        this.gjelderForBoligIkkeTattIBruk = gjelderForBoligIkkeTattIBruk
        return this
    }

    fun withGjelderForBruktSomHelarsbolig(gjelderForBruktSomHelarsbolig: Boolean): BopliktomraadeBuilder {
        this.gjelderForBruktSomHelarsbolig = gjelderForBruktSomHelarsbolig
        return this
    }

    fun withGjelderForUbebygdBoligtomt(gjelderForUbebygdBoligtomt: Boolean): BopliktomraadeBuilder {
        this.gjelderForUbebygdBoligtomt = gjelderForUbebygdBoligtomt
        return this
    }

    fun withHarUnntakFraSlektskapsunntak(harUnntakFraSlektskapsunntak: Boolean): BopliktomraadeBuilder {
        this.harUnntakFraSlektskapsunntak = harUnntakFraSlektskapsunntak
        return this
    }

    fun withAndreLokaleAvgrensninger(andreLokaleAvgrensninger: String?): BopliktomraadeBuilder {
        this.andreLokaleAvgrensninger = andreLokaleAvgrensninger
        return this
    }
}
