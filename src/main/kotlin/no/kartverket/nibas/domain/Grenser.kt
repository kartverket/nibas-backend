package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.metadata.Dokref
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import org.hibernate.envers.Audited
import org.locationtech.jts.geom.LineString
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.*
import java.util.logging.Logger
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
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
    // Blir mappet av Converter
    var grensetype: Grensetype,

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()) : CommonMetadataProperties, MaritimGrense, Laasbar {

    @Transient
    private val logger: Logger = Logger.getLogger(this::class.java.name)

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @Embedded
    override var gyldighet: Gyldighet = Gyldighet()

    @Embedded
    var endringsflagg: Endringsflagg? = Endringsflagg()

    @Embedded
    var registreringsversjon: Registreringsversjon? = Registreringsversjon()

    @Embedded
    var kopidata: Kopidata? = Kopidata()

    @Embedded
    var kvalitet: PosisjonskvalitetEmbeddable? = PosisjonskvalitetEmbeddable()

    // For KommunalKretsgrense
    @Embedded
    var foelgerGrunnkretsgrenseIdentifikasjon: FoelgerGrunnkretsgrenseIdentifikasjon? = FoelgerGrunnkretsgrenseIdentifikasjon()

    @Version
    override var version: Int = 1

    override var objtype: String? = grensetype.value

    @Column(name = "lokalid", nullable = false, updatable = false)
    override var lokalid: String = UUID.randomUUID().toString()

    @Column(name = "navnerom", nullable = false, updatable = false)
    override var navnerom: String = DEFAULT_NAVNEROM

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "datafangstdato")
    override var datafangstdato: LocalDate? = null

    @Column(name = "opphav")
    override var opphav: String? = null

    @Column(name = "grense", nullable = false)
    lateinit var grense: LineString

    @Column(name = "info")
    var informasjonstekst: String? = null

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: LocalDate? = null

    @Column(name = "omtvistet")
    var omtvistet: Boolean? = null

    @Column(name = "gjelderomraade")
    override var gjelderomraade: String? = null

    @Column(name = "virkeomraadenavn")
    override var virkeomraadenavn: String? = null

    @Column(name = "transformasjonsdato")
    override var transformasjonsdato: LocalDate? = null

    @Column(name = "beregningsdato")
    var beregningsdato: LocalDate? = null

    @Column(name = "stedfestingverifisert")
    var stedfestingverifisert: Boolean? = null

    @Column(name = "grensegatebredde")
    var grensegatebredde: Double? = null

    // UUID
    @Column(name = "kommunenummer")
    var kommunenummerId: String? = null

    // Holder på liste av kommunenummer-UUIDs grensen er tilknyttet
    // Uten at vi behøver definere egen entity for dette.
    // Brukes av grenser av type Lovvirkeomraadegrense
    @ElementCollection
    var kommunenummer: MutableSet<String> = mutableSetOf()

    // For statistiske grenser
    @Column(name = "foelgerteiggrense")
    var foelgerteiggrense: Boolean? = null

    @ManyToOne
    @JoinColumn(name = "foelgerterrengdetalj")
    var foelgerterrengdetalj: Terrengdetaljkode? = null

    @ManyToOne
    @JoinColumn(name = "noeyaktighetsklasse")
    var noeyaktighetsklasse: Noeyaktighetsklassekode? = null

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    var grensestatus: Grensestatus? = null

    @ManyToOne
    @JoinColumn(name = "fastsettingstype")
    var fastsettingstype: Fastsettingstype? = null

    @OneToMany(mappedBy = "grenseFk", cascade = [CascadeType.ALL])
    var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    @OneToMany(mappedBy = "grenseFk", cascade = [CascadeType.ALL])
    var ansvarligeMyndigheter: MutableSet<GrenseAnsvarligMyndighet> = mutableSetOf()

    // Relasjon oppover i treet til Grunnkrets
    @OneToMany(mappedBy = "grense")
    var grunnkretser: MutableSet<GrunnkretsGrenseKobling> = mutableSetOf()

    // Relasjon oppover i treet til Stemmekrets
    @OneToMany(mappedBy = "grense")
    var stemmekretser: MutableSet<StemmekretsGrenseKobling> = mutableSetOf()

    @ManyToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        schema = "nibas",
        name = "grense_land",
        joinColumns = [JoinColumn(name = "grense_fk")],
        inverseJoinColumns = [JoinColumn(name = "landkode_fk")]
    )
    var landkoder: MutableSet<Landkode> = mutableSetOf()

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

    override fun getAnsvarligeMyndigheter(): Collection<AnsvarligMyndighet> {
        return this.ansvarligeMyndigheter
    }

    override fun setAnsvarligeMyndigheter(ansvarligeMyndigheter: List<AnsvarligMyndighetHolder>) {
        doSetAnsvarligeMyndigheter(ansvarligeMyndigheter, this.ansvarligeMyndigheter) { GrenseAnsvarligMyndighet() }
    }

    override fun getLandIds(): List<String> {
        return this.landkoder.map { it.id }
    }

    override fun setLand(landkoder: List<Landkode>) {
        this.landkoder.clear()
        this.landkoder.addAll(landkoder)
    }

    fun addDokumentasjonsreferanse(): Dokumentasjonsreferanse {
        val dokumentasjonsreferanse = Dokumentasjonsreferanse()
        this.dokumentasjonsreferanser.add(dokumentasjonsreferanse)
        dokumentasjonsreferanse.grenseFk = this
        return dokumentasjonsreferanse
    }

    fun updateDokumentasjonsreferanser(dokRefList: List<Dokref>) {
        dokRefList.forEach { dokRef ->
            var dokumentasjonsreferanse = this.dokumentasjonsreferanser.find { dokumentasjonsreferanse -> dokumentasjonsreferanse.id == dokRef.id }
            if (dokumentasjonsreferanse == null) { // Legger til ny Dokumentasjonsreferanse hvis id er null
                if (dokRef.id != null) { // Har sendt inn ikke-eksisterende id: Kaster HTTP 400
                    logger.warning("Invalid request - non-existing Dokumentasjonsreferanse-id.")
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-existing Dokumentasjonsreferanse-id")
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
                        logger.warning("Invalid request - non-existing DokumentasjonsreferanseDokumentlink-id.")
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-existing DokumentasjonsreferanseDokumentlink-id")
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
                        logger.warning("Invalid request - non-existing DokumentasjonsreferanseInternReferanseKartverket-id.")
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-existing DokumentasjonsreferanseInternReferanseKartverket-id")
                    }
                    internReferanse = dokumentasjonsreferanse.addInternReferanse()
                }
                internReferanse.internReferanseKartverket = internRefHolder.beskrivelse
            }
        }
    }
}

@Table(name = "grense_ansvarligmyndighet")
@Audited
@Entity
class GrenseAnsvarligMyndighet : AnsvarligMyndighet {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "grense_fk")
    var grenseFk: Grense? = null

    @Column(name = "ansvarligmyndighet")
    override var ansvarligmyndighet: String? = null
}
