package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.Fastsettingstype
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Grensestatus
import no.kartverket.nibas.domain.Noeyaktighetsklassekode
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.domain.Terrengdetaljkode
import java.time.ZonedDateTime

const val LOKALID = "lokalId1"
const val NAVNEROM = "navnerom"
const val VERSJON_33 = "versjon33"
const val MYNDIGHET = "myndighet"
const val HJEMMEL_I_LOVEN = "Hjemmel i loven"
const val RETTSKILDE_ID_1 = "RettskildeId 1"
const val TITTEL = "Kilden til alt bra"
const val INFORMASJONTEKST = "Informasjon om det ene og andre."
const val LINK_TEKST_1 = "C. Link1"
const val LINK_TEKST_2 = "D. Link2"
const val REF_TEKST_1 = "B. Intern 1"
const val REF_TEKST_2 = "A. Intern 2"

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
    private var navnerom: String = NAVNEROM
    private var versjonid: String = VERSJON_33
    private var omtvistet: Boolean = true
    private var oppdateringsdato: ZonedDateTime = ZonedDateTime.now()
    private var datafangstdato: ZonedDateTime = ZonedDateTime.now()
    private var gyldigFra: ZonedDateTime = ZonedDateTime.now()
    private var dokumentasjonsreferanser: MutableList<Dokumentasjonsreferanse> = mutableListOf()
    private var kvalitet: PosisjonskvalitetEmbeddable = PosisjonskvalitetEmbeddable(
        maalemetode = MaalemetodeKodeEnum.TERRENGMAALT.uuid,
        noeyaktighet = 200
    )
    private var opphav = "opphav3"
    private var terrengdetaljEnum: TerrengdetaljEnum = TerrengdetaljEnum.AnnetGjerde
    private var noeyaktighetsklasseEnum: NoeyaktighetsklasseEnum = NoeyaktighetsklasseEnum.NK1
    private var fastsettingstypeEnum: FastsettingstypeEnum = FastsettingstypeEnum.BestemteKoordinater
    private var grensestatusEnum: GrensestatusEnum = GrensestatusEnum.Traktat
    private var informasjonstekst = INFORMASJONTEKST

    fun build(): Fylkesgrense {
        val fylkesgrense = Fylkesgrense(id = this.id)
        fylkesgrense.lokalid = lokalid
        fylkesgrense.navnerom = navnerom
        fylkesgrense.versjonid = versjonid
        fylkesgrense.foelgerterrengdetalj = Terrengdetaljkode().apply { id = terrengdetaljEnum.code }
        fylkesgrense.noeyaktighetsklasse = Noeyaktighetsklassekode().apply { id = noeyaktighetsklasseEnum.code }
        fylkesgrense.omtvistet = omtvistet
        fylkesgrense.datafangstdato = datafangstdato
        fylkesgrense.gyldigFra = gyldigFra
        fylkesgrense.gyldigTil = null
        fylkesgrense.kvalitet = kvalitet
        fylkesgrense.oppdateringsdato = oppdateringsdato
        fylkesgrense.opphav = opphav
        fylkesgrense.fastsettingstype = Fastsettingstype().apply { id = fastsettingstypeEnum.name }
        fylkesgrense.grensestatus = Grensestatus().apply { id = grensestatusEnum.name }
        fylkesgrense.setInfo(informasjonstekst)

        fylkesgrense.dokumentasjonsreferanser.addAll(dokumentasjonsreferanser)
        return fylkesgrense
    }

    fun withOppdateringsdato(dateTime: ZonedDateTime): FylkesgrenseBuilder {
        this.oppdateringsdato = dateTime
        return this
    }

    fun datafangstdato(dateTime: ZonedDateTime): FylkesgrenseBuilder {
        this.datafangstdato = dateTime
        return this
    }

    fun gyldigFra(dateTime: ZonedDateTime): FylkesgrenseBuilder {
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
