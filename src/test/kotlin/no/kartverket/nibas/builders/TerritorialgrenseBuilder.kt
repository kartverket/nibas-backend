package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.AnsvarligMyndighetHolder
import no.kartverket.nibas.domain.DokrefGrenseKobling
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Landkode
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.sporing.Gyldighet
import java.time.LocalDate
import java.time.ZonedDateTime

class TerritorialgrenseBuilder {

    private var lokalid: String = LOKALID
    private var endretdato: ZonedDateTime = ZonedDateTime.now()
    private var datafangstdato: ZonedDateTime = ZonedDateTime.now()
    private var gyldigFra: LocalDate = LocalDate.now()
    private var dokumentasjonsreferanser: MutableList<Dokumentasjonsreferanse> = mutableListOf()
    private var kvalitet: PosisjonskvalitetEmbeddable = PosisjonskvalitetEmbeddable(
        maalemetode = MaalemetodeKodeEnum.TERRENGMAALT.uuid,
        noeyaktighet = 200
    )
    private var opphav = OPPHAV
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

    fun build(): Grense {
        val territorialgrense = Grense(lokalid = lokalid, grensetype = Grensetype.Territorialgrense)
        territorialgrense.datafangstdato = datafangstdato
        territorialgrense.gyldighet = Gyldighet(gyldigFra, null)
        territorialgrense.kvalitet = kvalitet
        territorialgrense.auditInfo.endretdato = endretdato
        territorialgrense.opphav = opphav
        territorialgrense.setInfo(informasjonstekst)
        territorialgrense.dokrefGrensekobling.addAll(dokumentasjonsreferanser.map {
            DokrefGrenseKobling().apply {
                dokumentasjonsreferanse = it
                grense = territorialgrense
            }
        }
        )
        return territorialgrense
    }

    fun withEndretDato(zonedDateTime: ZonedDateTime): TerritorialgrenseBuilder {
        this.endretdato = zonedDateTime
        return this
    }

    fun datafangstdato(zonedDateTime: ZonedDateTime): TerritorialgrenseBuilder {
        this.datafangstdato = zonedDateTime
        return this
    }

    fun gyldigFra(dateTime: LocalDate): TerritorialgrenseBuilder {
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
