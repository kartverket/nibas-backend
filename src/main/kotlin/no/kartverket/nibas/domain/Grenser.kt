package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.metadata.Dokref
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import org.locationtech.jts.geom.LineString
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version

/**
 * Felles Grense-tabell som holder på samtlige typer av grenser skilt på Grensetype.
 */
@Table(name = "grense")
@Entity
class Grense(
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString(),
    @Column(name = "lokalid", nullable = false, updatable = false)
    override val lokalid: String = UUID.randomUUID().toString(),
    // Blir mappet av Converter
    @Column(name = "avgrensningstype")
    var grensetype: Grensetype,
) : CommonMetadataProperties, Laasbar {

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @Embedded
    override var gyldighet: Gyldighet = Gyldighet()

    @Embedded
    var kvalitet: PosisjonskvalitetEmbeddable? = PosisjonskvalitetEmbeddable()

    @Version
    override var version: Int = 1

    var objtype: String? = grensetype.value

    @Column(name = "datafangstdato")
    override var datafangstdato: ZonedDateTime? = null

    @Column(name = "foerstedigitaliseringsdato", nullable = false, updatable = false)
    override val foerstedigitaliseringsdato: ZonedDateTime = ZonedDateTime.now()

    @Column(name = "opphav")
    override var opphav: String? = null

    @Column(name = "grense", nullable = false)
    lateinit var grense: LineString

    @Column(name = "info")
    var informasjonstekst: String? = null

    // Holder på liste av kommunenummer-UUIDs grensen er tilknyttet
    // Uten at vi behøver definere egen entity for dette.
    // Brukes av grenser av type Lovvirkeomraadegrense
    @ElementCollection
    var kommunenummer: MutableSet<String> = mutableSetOf()

    @ManyToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name = "grense_dokumentasjonsreferanse",
        joinColumns = [JoinColumn(name = "grense_fk")],
        inverseJoinColumns = [JoinColumn(name = "dokumentasjonsreferanse_fk")]
    )
    var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    // Relasjon oppover i treet til Grunnkrets
    @OneToMany(mappedBy = "grense")
    var grunnkretser: MutableSet<GrunnkretsGrenseKobling> = mutableSetOf()

    // Relasjon oppover i treet til Stemmekrets
    @OneToMany(mappedBy = "grense")
    var stemmekretser: MutableSet<StemmekretsGrenseKobling> = mutableSetOf()

    @OneToOne
    @JoinColumn(name = "neste_versjon")
    var nesteVersjon: Grense? = null

    fun grunnkretser(): Set<Grunnkrets> = grunnkretser.map { it.grunnkrets }.toSet()
    fun stemmekretser(): Set<Stemmekrets> = stemmekretser.map { it.stemmekrets }.toSet()
    fun kommuner(): Set<Kommune> = grunnkretser().map { it.kommune }.toSet()

    fun fylker(): Set<Fylke> = kommuner().map { it.fylke }.toSet()

    fun nasjoner(): Set<Nasjon> = fylker().map { it.nasjon }.toSet()

    fun finnFlaterOppTilGrunnkrets(): Set<Flate> = grunnkretser()

    fun finnFlaterOppTilKommune(): Set<Flate> = HashSet(finnFlaterOppTilGrunnkrets() + kommuner())

    fun finnFlaterOppTilFylke(): Set<Flate> = HashSet(finnFlaterOppTilKommune() + fylker())

    fun finnFlaterOppTilNasjon(): Set<Flate> = HashSet(finnFlaterOppTilFylke() + nasjoner())

    fun finnFlaterOppTilStemmekrets(): Set<Flate> = stemmekretser()

    override fun getInfo(): String? {
        return informasjonstekst
    }

    override fun setInfo(updatedInfo: String?) {
        this.informasjonstekst = updatedInfo
    }

    fun addDokumentasjonsreferanse(): Dokumentasjonsreferanse {
        val dokumentasjonsreferanse = Dokumentasjonsreferanse()
        this.dokumentasjonsreferanser.add(dokumentasjonsreferanse)
        return dokumentasjonsreferanse
    }

    fun updateDokumentasjonsreferanser(dokRefList: List<Dokref>) {
        //TODO mangler gyldighet på dokumentasjonsreferanser
        dokRefList.forEach { dokRef ->
            val dokRefId = dokRef.id
            var dokumentasjonsreferanse = this.dokumentasjonsreferanser.find { dokumentasjonsreferanse -> dokumentasjonsreferanse.id == dokRefId }
            if (dokumentasjonsreferanse == null) { // Legger til ny Dokumentasjonsreferanse hvis id er null
                if (dokRefId != null) { // Har sendt inn ikke-eksisterende id: Kaster HTTP 400
                    throw BadRequestException("Non-existing Dokumentasjonsreferanse-id: $dokRefId")
                }
                dokumentasjonsreferanse = addDokumentasjonsreferanse()
            }

            dokumentasjonsreferanse.fastsettingsdato = dokRef.fastsettingsdato
            dokumentasjonsreferanse.fastsettingsmyndighet = dokRef.fastsettingsmyndighet
            dokumentasjonsreferanse.hjemmel = dokRef.hjemmel
            dokumentasjonsreferanse.rettskildeid = dokRef.rettskildeId
            dokumentasjonsreferanse.rettskildetittel = dokRef.rettskildeTittel

            val dokumentlenker = dokumentasjonsreferanse.dokumentlenker
            dokRef.dokumentlenker.forEach { dokumentLenkeHolder ->
                val dokumentLenkeHolderId = dokumentLenkeHolder.id
                var dokumentLink = dokumentlenker.find { dokumentLink -> dokumentLink.id == dokumentLenkeHolderId }
                if (dokumentLink == null) { // Hvis ny instans, opprettes den
                    if (dokumentLenkeHolderId != null) { // Har sendt inn ikke-eksisterende id: Kaster HTTP 400
                        throw BadRequestException("Non-existing DokumentasjonsreferanseDokumentlink-id: $dokumentLenkeHolderId")
                    }
                    dokumentLink = dokumentasjonsreferanse.addDokumentLenke()
                }
                dokumentLink.dokumentlink = dokumentLenkeHolder.beskrivelse
            }

            val internReferanser = dokumentasjonsreferanse.internReferanserKartverket
            dokRef.internReferanserKartverket.forEach { internRefHolder ->
                val internRefHolderId = internRefHolder.id
                var internReferanse = internReferanser.find { internRef -> internRef.id == internRefHolderId }
                if (internReferanse == null) { // Hvis ny instans, opprettes den
                    if (internRefHolderId != null) { // Har sendt inn ikke-eksisterende id: Kaster HTTP 400
                        throw BadRequestException("Non-existing DokumentasjonsreferanseInternReferanseKartverket-id: $internRefHolderId")
                    }
                    internReferanse = dokumentasjonsreferanse.addInternReferanse()
                }
                internReferanse.internReferanseKartverket = internRefHolder.beskrivelse
            }
        }
    }

    /**
     * Sjekker om enten seg selv eller en av Grensene som den peker til er gyldig på dato og matcher grensetyper.
     */
    fun finnRelevantGrenseForTyperOgDato(grensetyper: Set<Grensetype>, gyldighetsdato: LocalDate = LocalDate.now()): Grense? {
        var grenseInstansToCheck: Grense? = this
        var result: Grense? = null
        while (result == null && grenseInstansToCheck != null) {
            if (grenseInstansToCheck.gyldigPaaDato(gyldighetsdato)) {
                if (grensetyper.isNotEmpty()) { // Hvis grensetyper er angitt må også sjekke for match på det
                    if (grenseInstansToCheck.harGrenseTypeContainedInSet(grensetyper)) {
                        result = grenseInstansToCheck
                    }
                } else {
                    result = grenseInstansToCheck
                }
            }
            grenseInstansToCheck = grenseInstansToCheck.nesteVersjon
        }
        return result
    }

    fun finnRelevantGrenseForDato(gyldighetsdato: LocalDate = LocalDate.now()): Grense? {
        return finnRelevantGrenseForTyperOgDato(emptySet(), gyldighetsdato)
    }

    fun erRelevantGrenseForTyperOgDato(grensetyper: Set<Grensetype>, gyldighetsdato: LocalDate = LocalDate.now()): Boolean =
        finnRelevantGrenseForTyperOgDato(grensetyper, gyldighetsdato) != null

    fun harGrenseTypeContainedInSet(grensetyper: Set<Grensetype>): Boolean = grensetyper.contains(this.grensetype)

    fun applyMappingFromExistingGrense(gammelGrense: Grense): Grense {
        return this.apply {
            gammelGrense.gyldighet.gyldigTil?.let { setGyldigTil(it) }
            datafangstdato = gammelGrense.datafangstdato
            opphav = gammelGrense.opphav
            grensetype = gammelGrense.grensetype
            kvalitet = gammelGrense.kvalitet
            setInfo(gammelGrense.informasjonstekst)

            grense = gammelGrense.grense
        }
    }

    fun applyCascadingOperationsBasedOnExistingGrense(gammelGrense: Grense): Grense {
        return this.apply {
            dokumentasjonsreferanser.addAll(gammelGrense.dokumentasjonsreferanser)
        }
    }
}
