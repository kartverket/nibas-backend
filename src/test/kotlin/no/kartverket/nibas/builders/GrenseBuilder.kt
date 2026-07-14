package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.DokrefGrenseKobling
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import java.time.LocalDate
import java.time.ZonedDateTime

enum class MaalemetodeKodeEnum(val uuid: String) {
    TERRENGMAALT("7f48625b-e46f-413e-ae4d-0381ac64264b")
}

enum class NoeyaktighetsklasseEnum(val code: String) {
    NK1("LiteNøyaktigeGrenserOgGrenserFraN50")
}

enum class TerrengdetaljEnum(val code: String) {
    AnnetGjerde("AGJ")
}

enum class FastsettingstypeEnum { BestemteKoordinater }
enum class GrensestatusEnum { Traktat }

class GrenseBuilder(private val id: String = "1",
                    private var lokalId: Lokalid = Lokalid("123"),
                    private var grenseGeometri: LineString = GeometryFactory().createLineString(),
                    private var gyldigFra: LocalDate = LocalDate.now(),
                    private var gyldigTil: LocalDate? = null,
                    private var kvalitet: PosisjonskvalitetEmbeddable = PosisjonskvalitetEmbeddable(),
                    private var opphav: String? = "Eidsvoll",
                    private var versjon: Int = 1,
                    private var grensetype: Grensetype = Grensetype.Grunnkretsgrense) {

    fun build(): Grense {
        val grense = Grense(id, lokalId.value, grensetype)
        grense.grense = grenseGeometri
        grense.gyldighet = Gyldighet(gyldigFra, gyldigTil)
        grense.opphav = opphav
        grense.version = versjon
        grense.kvalitet = kvalitet

        return grense
    }

    fun withLokalId(lokalId: String): GrenseBuilder {
        this.lokalId = Lokalid(lokalId)
        return this
    }

    fun withKvalitet(maalemetode: MaalemetodeKodeEnum, noyaktighet: Int): GrenseBuilder {
        val posisjonskvalitetEmbeddable = PosisjonskvalitetEmbeddable(
            maalemetode = maalemetode.uuid,
            noeyaktighet = noyaktighet,
        )
        this.kvalitet = posisjonskvalitetEmbeddable
        return this
    }

    fun withGrenseGeometri(coordinates: List<Coordinate>): GrenseBuilder {
        this.grenseGeometri = GeometryFactory().createLineString(coordinates.toTypedArray())
        return this
    }

    fun withGrenseGeometri(vararg punkter: Point): GrenseBuilder {
        this.grenseGeometri = GeometryFactory().createLineString(punkter.map { punkt -> punkt.coordinate }.toTypedArray())
        return this
    }

    fun withVersjon(versjon: Int): GrenseBuilder {
        this.versjon = versjon
        return this
    }

    fun withGrensetype(grensetype: Grensetype): GrenseBuilder {
        this.grensetype = grensetype
        return this
    }

    fun withGyldigFra(dato: LocalDate): GrenseBuilder {
        this.gyldigFra = dato
        return this
    }

    fun withGyldigTil(dato: LocalDate): GrenseBuilder {
        this.gyldigTil = dato
        return this
    }

}

const val LOKALID = "lokalId1"
const val MYNDIGHET = "myndighet"
const val HJEMMEL_I_LOVEN = "Hjemmel i loven"
const val RETTSKILDE_ID_1 = "RettskildeId 1"
const val TITTEL = "Kilden til alt bra"
const val INFORMASJONTEKST = "Informasjon om det ene og andre."
const val LINK_TEKST_1 = "C. Link1"
const val LINK_TEKST_2 = "D. Link2"
const val REF_TEKST_1 = "B. Intern 1"
const val REF_TEKST_2 = "A. Intern 2"
const val OPPHAV = "Opphav må til"

class FylkesgrenseBuilder(private val id: String = "1",
                          private var lokalid: Lokalid = Lokalid(LOKALID),
                          private var omtvistet: Boolean = true,
                          private var endretdato: ZonedDateTime = ZonedDateTime.now(),
                          private var datafangstdato: ZonedDateTime = ZonedDateTime.now(),
                          private var gyldigFra: LocalDate = LocalDate.now(),
                          private var gyldigTil: LocalDate? = null,
                          private var dokumentasjonsreferanser: MutableList<Dokumentasjonsreferanse> = mutableListOf(),
                          private var kvalitet: PosisjonskvalitetEmbeddable = PosisjonskvalitetEmbeddable(
                              maalemetode = MaalemetodeKodeEnum.TERRENGMAALT.uuid,
                              noeyaktighet = 200,
                          ),
                          private var opphav: String = OPPHAV,
                          private var informasjonstekst: String = INFORMASJONTEKST,
                          private var grenseGeometri: LineString = GeometryFactory().createLineString()) {

    fun build(): Grense {
        val fylkesgrense = Grense(id = this.id, lokalid = lokalid.value, Grensetype.Fylkesgrense)
        fylkesgrense.datafangstdato = datafangstdato
        fylkesgrense.gyldighet = Gyldighet(gyldigFra, gyldigTil)
        fylkesgrense.kvalitet = kvalitet
        fylkesgrense.auditInfo.endretdato = endretdato
        fylkesgrense.opphav = opphav
        fylkesgrense.informasjon = informasjonstekst
        fylkesgrense.grense = grenseGeometri

        fylkesgrense.dokrefGrensekobling.addAll(dokumentasjonsreferanser.map {
            DokrefGrenseKobling().apply {
                dokumentasjonsreferanse = it
                grense = fylkesgrense
            }
        })
        return fylkesgrense
    }

    fun withEndretDato(zonedDateTime: ZonedDateTime): FylkesgrenseBuilder {
        this.endretdato = zonedDateTime
        return this
    }

    fun datafangstdato(zonedDateTime: ZonedDateTime): FylkesgrenseBuilder {
        this.datafangstdato = zonedDateTime
        return this
    }

    fun gyldigFra(dateTime: LocalDate): FylkesgrenseBuilder {
        this.gyldigFra = dateTime
        return this
    }

    fun dokRef(dokRef: Dokumentasjonsreferanse): FylkesgrenseBuilder {
        this.dokumentasjonsreferanser.add(dokRef)
        return this
    }

    fun informasjon(tekst: String): FylkesgrenseBuilder {
        this.informasjonstekst = tekst
        return this
    }
}
