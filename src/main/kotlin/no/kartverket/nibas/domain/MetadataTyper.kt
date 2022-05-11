package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.geojson.metadata.KodelisteEntry
import no.kartverket.nibas.api.v1.common.geojson.metadata.KodelisteHrefProvider
import no.kartverket.nibas.api.v1.common.geojson.metadata.KodelisteHrefProviderI
import no.kartverket.nibas.api.v1.common.geojson.metadata.Posisjonskvalitet
import no.kartverket.nibas.sporing.Sporbar
import java.time.LocalDate
import java.util.function.Supplier
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Embeddable
data class Endringsflagg(
    @ManyToOne
    @JoinColumn(name = "typeendring")
    var typeendring: Typeendring? = null,

    @Column(name = "tidspunktendring")
    var tidspunktendring: LocalDate? = null
)

@Embeddable
data class Kopidata(
    @Column(name = "omraadeid")
    var omraadeid: Int? = null,

    @Column(name = "originaldatavert")
    var originaldatavert: String? = null,

    @Column(name = "kopidato")
    var kopidato: LocalDate? = null
)

@Embeddable
data class PosisjonskvalitetEmbeddable(
    @Column(name = "maalemetode", nullable = false)
    var maalemetode: String = "",
    @Column(name = "noeyaktighet")
    var noeyaktighet: Int? = null
) {
    fun toKvalitet(kodelisteHrefProvider: KodelisteHrefProviderI = KodelisteHrefProvider()): Posisjonskvalitet = Posisjonskvalitet(
        KodelisteEntry(
            id = maalemetode,
            href = kodelisteHrefProvider.maalemetodeKodeHref()),
        noeyaktighet)
}

@Embeddable
data class Registreringsversjon(
    @Column(name = "produkt")
    var produkt: String? = null,

    @Column(name = "versjon")
    var versjon: String? = null
)

// Felles egenskaper for grenser, adm_enheter, punkter ++
interface CommonMetadataProperties : ContainsSosiIdentifikasjon, Sporbar {
    var datafangstdato: LocalDate?
    var gyldigFra: LocalDate?
    var gyldigTil: LocalDate?
    var opphav: String?
    fun getInfo(): String?
    fun setInfo(updatedInfo: String?)
}

// Felles egenskaper for maritime grenser
interface MaritimGrense {
    var gjelderomraade: String?
    var transformasjonsdato: LocalDate?
    var virkeomraadenavn: String?
    fun getAnsvarligeMyndigheter(): Collection<AnsvarligMyndighet>
    fun setAnsvarligeMyndigheter(ansvarligeMyndigheter: List<AnsvarligMyndighetHolder>)
    fun getLandIds(): List<String>
    fun setLand(landkoder: List<Landkode>)
}

// Representerer domenemodellen, slipper ut av backend.
interface AnsvarligMyndighet {
    val id: String
    var ansvarligmyndighet: String?
}

// Representerer domenemodellen, slipper ut av backend.
interface Info {
    val id: String
    var informasjon: String?
}

// Innkommende fra klienten. Id kan være null
data class AnsvarligMyndighetHolder(val id: String? = null, var ansvarligmyndighet: String? = null)

/**
 * Hjelpefunksjon for å sette informasjonstekst inn i liste av infoelementer (skal i praksis være en singleton-collection).
 * Dersom innsendt informasjon er null, og koblingsentiteten finnes, så blir feltet bare satt til null, vi sletter ikke
 * koblingen. (Vi kan gjøre det med removeOrphans.)
 * @param updatedInfo Ny eller endret info som man ønsker å sette på entiteten. Kan også være null.
 * @param existingInfoElementer Collection av info-elementene som sitter på entiteten fra før
 * @param newInfoSupplier Supplier for å kunne instansiere ny Info-impl
 */
fun <T : Info> doSetInfoInSingletonCollection(
    updatedInfo: String?,
    existingInfoElementer: MutableSet<T>,
    newInfoSupplier: Supplier<T>
) {
    if (updatedInfo == null && existingInfoElementer.isNotEmpty()) {
        existingInfoElementer.first().informasjon = null
    } else {
        val infoElement: T
        if (existingInfoElementer.isEmpty()) {
            infoElement = newInfoSupplier.get()
            existingInfoElementer.add(infoElement)
        } else {
            infoElement = existingInfoElementer.first()
        }
        infoElement.informasjon = updatedInfo
    }
}

fun <T : Info> doGetInfoFromSingletonCollection(informasjonselementer: MutableSet<T>): String? {
    return informasjonselementer.firstOrNull()?.informasjon
}

/**
 * Hjelpefunksjon for å sette/oppdatere liste av ansvarlige myndigheter.
 * @param listToUpdate Liste med ansvarlige myndigheter som man ønsker å sette i objektet
 * @param existingAnsvarligeMyndigheter Collection av de ansvarlige myndigheter som sitter på entiteten fra før
 * @param newAnsvarligMyndighetSupplier Supplier for å kunne instansiere ny AnsvarligMyndighet-impl
 */
fun <T : AnsvarligMyndighet> doSetAnsvarligeMyndigheter(
    listToUpdate: List<AnsvarligMyndighetHolder>,
    existingAnsvarligeMyndigheter: MutableSet<T>,
    newAnsvarligMyndighetSupplier: Supplier<T>
) {
    listToUpdate.forEach { ansvarligMyndighetHolder ->
        var ansvarligMyndighetEntity = existingAnsvarligeMyndigheter.find { am -> am.id == ansvarligMyndighetHolder.id }
        if (ansvarligMyndighetEntity == null) { // Hvis ny instans, opprettes den
            ansvarligMyndighetEntity = newAnsvarligMyndighetSupplier.get()
            existingAnsvarligeMyndigheter.add(ansvarligMyndighetEntity)
        }
        ansvarligMyndighetEntity.ansvarligmyndighet = ansvarligMyndighetHolder.ansvarligmyndighet
    }
}
