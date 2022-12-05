package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import java.time.LocalDate
import java.time.ZonedDateTime

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


class FylkesgrenseBuilder(private val id: String = "1") {

    private var lokalid: String = LOKALID
    private var omtvistet: Boolean = true
    private var endretdato: ZonedDateTime = ZonedDateTime.now()
    private var datafangstdato: ZonedDateTime = ZonedDateTime.now()
    private var gyldigFra: LocalDate = LocalDate.now()
    private var dokumentasjonsreferanser: MutableList<Dokumentasjonsreferanse> = mutableListOf()
    private var kvalitet: PosisjonskvalitetEmbeddable = PosisjonskvalitetEmbeddable(
        maalemetode = MaalemetodeKodeEnum.TERRENGMAALT.uuid,
        noeyaktighet = 200
    )
    private var opphav = OPPHAV
    private var informasjonstekst = INFORMASJONTEKST
    private var grenseGeometri: LineString = GeometryFactory().createLineString()

    fun build(): Grense {
        val fylkesgrense = Grense(id = this.id , lokalid= lokalid, Grensetype.Fylkesgrense)
        fylkesgrense.datafangstdato = datafangstdato
        fylkesgrense.gyldighet = Gyldighet(gyldigFra, null)
        fylkesgrense.kvalitet = kvalitet
        fylkesgrense.auditInfo.endretdato = endretdato
        fylkesgrense.opphav = opphav
        fylkesgrense.setInfo(informasjonstekst)
        fylkesgrense.grense = grenseGeometri

        fylkesgrense.dokumentasjonsreferanser.addAll(dokumentasjonsreferanser)
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
        return this;
    }
}
