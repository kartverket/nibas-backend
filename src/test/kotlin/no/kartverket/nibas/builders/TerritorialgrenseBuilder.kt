package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.AnsvarligMyndighetHolder
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.Fastsettingstype
import no.kartverket.nibas.domain.Grensestatus
import no.kartverket.nibas.domain.Landkode
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.domain.Territorialgrense
import java.time.LocalDate
import java.time.ZonedDateTime

class TerritorialgrenseBuilder {

    private var lokalid: String = LOKALID
    private var navnerom: String = NAVNEROM
    private var versjonid: String = VERSJON_33
    private var oppdateringsdato: ZonedDateTime = ZonedDateTime.now()
    private var datafangstdato: ZonedDateTime = ZonedDateTime.now()
    private var gyldigFra: ZonedDateTime = ZonedDateTime.now()
    private var dokumentasjonsreferanser: MutableList<Dokumentasjonsreferanse> = mutableListOf()
    private var kvalitet: PosisjonskvalitetEmbeddable = PosisjonskvalitetEmbeddable(
        maalemetode = MaalemetodeKodeEnum.TERRENGMAALT.uuid,
        noeyaktighet = 200
    )
    private var opphav = "opphav3"
    private var fastsettingstypeEnum: FastsettingstypeEnum = FastsettingstypeEnum.BestemteKoordinater
    private var grensestatusEnum: GrensestatusEnum = GrensestatusEnum.Traktat
    private var informasjonstekst = INFORMASJONTEKST
    private var transformasjonsdato: LocalDate = LocalDate.now()
    private var gjelderOmraade: String = "Område over elva"
    private var ansvarligeMyndigheter: MutableList<AnsvarligMyndighetHolder> = mutableListOf()
    private var virkeomraade = "Virkeområde 33"
    private val ru: Landkode = Landkode().apply { id = "ru"; description = "Russland" }
    private var landkoder: MutableList<Landkode> = mutableListOf(ru)
    private var beregningsdato: LocalDate = LocalDate.now()

    fun build(): Territorialgrense {
        val territorialgrense = Territorialgrense()
        territorialgrense.lokalid = lokalid
        territorialgrense.navnerom = navnerom
        territorialgrense.versjonid = versjonid
        territorialgrense.datafangstdato = datafangstdato
        territorialgrense.gyldigFra = gyldigFra
        territorialgrense.gyldigTil = null
        territorialgrense.kvalitet = kvalitet
        territorialgrense.oppdateringsdato = oppdateringsdato
        territorialgrense.opphav = opphav
        territorialgrense.fastsettingstype = Fastsettingstype().apply { id = fastsettingstypeEnum.name }
        territorialgrense.grensestatus = Grensestatus().apply { id = grensestatusEnum.name }
        territorialgrense.setInfo(informasjonstekst)
        territorialgrense.dokumentasjonsreferanser.addAll(dokumentasjonsreferanser)
        territorialgrense.transformasjonsdato = transformasjonsdato
        territorialgrense.gjelderomraade = gjelderOmraade
        territorialgrense.setAnsvarligeMyndigheter(ansvarligeMyndigheter)
        territorialgrense.virkeomraadenavn = virkeomraade
        territorialgrense.setLand(landkoder)
        territorialgrense.beregningsdato = beregningsdato
        return territorialgrense
    }

    fun withOppdateringsdato(dateTime: ZonedDateTime): TerritorialgrenseBuilder {
        this.oppdateringsdato = dateTime
        return this
    }

    fun datafangstdato(dateTime: ZonedDateTime): TerritorialgrenseBuilder {
        this.datafangstdato = dateTime
        return this
    }

    fun gyldigFra(dateTime: ZonedDateTime): TerritorialgrenseBuilder {
        this.gyldigFra = dateTime
        return this
    }

    fun dokRef(dokRef: Dokumentasjonsreferanse): TerritorialgrenseBuilder {
        this.dokumentasjonsreferanser.add(dokRef)
        return this
    }

    fun informasjon(tekst: String): TerritorialgrenseBuilder {
        this.informasjonstekst = tekst
        return this
    }

    fun transformasjonsdato(transformasjonsdato: LocalDate): TerritorialgrenseBuilder {
        this.transformasjonsdato = transformasjonsdato
        return this
    }

    fun withAnsvarligeMyndigheter(vararg ansvarligeMyndigheter: AnsvarligMyndighetHolder): TerritorialgrenseBuilder {
        this.ansvarligeMyndigheter.addAll(ansvarligeMyndigheter)
        return this
    }

    fun beregningsdato(beregningsdato: LocalDate): TerritorialgrenseBuilder {
        this.beregningsdato = beregningsdato
        return this
    }

}
