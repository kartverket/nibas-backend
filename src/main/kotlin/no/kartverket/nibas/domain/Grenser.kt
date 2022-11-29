package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.metadata.Dokref
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import org.hibernate.envers.Audited
import org.locationtech.jts.geom.LineString
import java.time.LocalDate
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Version

/**
 * Felles Grense-tabell som holder på samtlige typer av grenser skilt på Grensetype.
 */
@Table(name = "grense")
@Audited
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
    override var datafangstdato: LocalDate? = null

    @Column(name = "foerstedigitaliseringsdato", nullable = false, updatable = false)
    override val foerstedigitaliseringsdato: LocalDate = LocalDate.now()

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
}
