package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.metadata.DokumentasjonsreferanseDTO
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import org.locationtech.jts.geom.LineString
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
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

    fun grunnkretser(gyldighetsdato: LocalDate = LocalDate.now()): Set<Grunnkrets> =
        grunnkretser.map { it.grunnkrets }.filter { it.gyldigPaaDato(gyldighetsdato) }.toSet()

    fun gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato: LocalDate = LocalDate.now()): Set<GrunnkretsGrenseKobling> =
        grunnkretser.filter { it.grunnkrets.gyldigPaaDato(gyldighetsdato) }.toSet()

    fun stemmekretser(gyldighetsdato: LocalDate = LocalDate.now()): Set<Stemmekrets> =
        stemmekretser.map { it.stemmekrets }.filter { it.gyldigPaaDato(gyldighetsdato) }.toSet()

    fun gyldigeStemmekretsGrenseKoblinger(gyldighetsdato: LocalDate = LocalDate.now()): Set<StemmekretsGrenseKobling> =
        stemmekretser.filter { it.stemmekrets.gyldigPaaDato(gyldighetsdato) }.toSet()

    fun kommuner(gyldighetsdato: LocalDate = LocalDate.now()): Set<Kommune> =
        grunnkretser(gyldighetsdato).mapNotNull { it.getKommune(gyldighetsdato) }.toSet()

    fun fylker(gyldighetsdato: LocalDate = LocalDate.now()): Set<Fylke> =
        kommuner(gyldighetsdato).mapNotNull { it.getFylke(gyldighetsdato) }.toSet()

    fun nasjoner(gyldighetsdato: LocalDate = LocalDate.now()): Set<Nasjon> =
        fylker(gyldighetsdato).mapNotNull { it.getNasjon(gyldighetsdato) }.toSet()

    fun finnFlaterOppTilKommune(gyldighetsdato: LocalDate = LocalDate.now()): Set<Flate> =
        HashSet(grunnkretser(gyldighetsdato) + kommuner(gyldighetsdato))

    fun finnFlaterOppTilFylke(gyldighetsdato: LocalDate = LocalDate.now()): Set<Flate> =
        HashSet(finnFlaterOppTilKommune(gyldighetsdato) + fylker(gyldighetsdato))

    fun finnFlaterOppTilNasjon(gyldighetsdato: LocalDate = LocalDate.now()): Set<Flate> =
        HashSet(finnFlaterOppTilFylke(gyldighetsdato) + nasjoner(gyldighetsdato))

    fun erAdministrativGrense(): Boolean {
        val adminstrativeGrensetyper = listOf(Grensetype.Kommunegrense, Grensetype.Fylkesgrense, Grensetype.Riksgrense, Grensetype.AvtaltAvgrensningslinje, Grensetype.Territorialgrense)
        return adminstrativeGrensetyper.contains(this.grensetype)
    }

    override fun getInfo(): String? {
        return informasjonstekst
    }

    override fun setInfo(updatedInfo: String?) {
        this.informasjonstekst = updatedInfo
    }

    fun addDokumentasjonsreferanse(lokalid: String? = null): Dokumentasjonsreferanse {
        val dokumentasjonsreferanse = lokalid?.let { Dokumentasjonsreferanse(lokalid = lokalid) } ?: Dokumentasjonsreferanse()
        this.dokumentasjonsreferanser.add(dokumentasjonsreferanse)
        return dokumentasjonsreferanse
    }

    private fun archiveAndRemoveDokumentasjonsreferanse(dokumentasjonsreferanse: Dokumentasjonsreferanse, gyldigTil: LocalDate) {
        dokumentasjonsreferanse.setGyldigTil(gyldigTil)
        this.dokumentasjonsreferanser.removeIf { dokumentasjonsreferanse.id == it.id }
    }

    private fun createNewVersionOfDokumentasjonsreferanse(dokumentasjonsreferanse: Dokumentasjonsreferanse, dokumentasjonsreferanseDTO: DokumentasjonsreferanseDTO): Dokumentasjonsreferanse? {
        if (dokumentasjonsreferanse.hasChanges(dokumentasjonsreferanseDTO)) {
            addDokumentasjonsreferanse(dokumentasjonsreferanse.lokalid).also {
                setDokrefFields(it, dokumentasjonsreferanseDTO)
                return it
            }
        }
        return null
    }

    fun updateDokumentasjonsreferanser(dokRefList: List<DokumentasjonsreferanseDTO>, gyldighetsdato: LocalDate, endringstype: Endringstype) {
        dokRefList.forEach { dokRef ->
            val dokRefId = dokRef.id
            val persistedDokumentasjonsreferanse = this.dokumentasjonsreferanser.find { dokumentasjonsreferanse -> dokRefId != null && dokumentasjonsreferanse.id == dokRefId }
            validateReferanseId(persistedDokumentasjonsreferanse, dokRefId, "Dokumentasjonsreferanse-id eksisterer ikke: $dokRefId")

            persistedDokumentasjonsreferanse?.let { oldVersion ->
                if (!dokRef.shouldArchive)
                    createNewVersionOfDokumentasjonsreferanse(persistedDokumentasjonsreferanse, dokRef)?.let { newVersion ->
                        newVersion.gyldighet = Gyldighet(gyldigFra = gyldighetsdato)
                        newVersion.auditInfo.typeEndring = endringstype
                        archiveAndRemoveDokumentasjonsreferanse(oldVersion, gyldighetsdato)
                    }
                else
                    archiveAndRemoveDokumentasjonsreferanse(oldVersion, gyldighetsdato)
            }

            persistedDokumentasjonsreferanse ?: addDokumentasjonsreferanse().run { setDokrefFields(this, dokRef) }
        }
    }

    private fun setDokrefFields(
        dokumentasjonsreferanse: Dokumentasjonsreferanse,
        dokRef: DokumentasjonsreferanseDTO
    ) {

        // Viderefører kun internreferanser og dokumentlenker som er med fra requesten.
        // Disse har 1..* forhold med dokumentasjonsreferansen, må kopieres uansett, og har ingen objid å videreføre.
        // Klienten støtter kun å slette eller legge til objekter av disse typene - ikke redigering.

        dokRef.dokumentlenker.forEach { lenke ->
            dokumentasjonsreferanse.addDokumentLenke().apply {
                this.dokumentlink = lenke.beskrivelse
            }
        }

        dokRef.internReferanserKartverket.forEach { lenke ->
            dokumentasjonsreferanse.addInternReferanse().apply {
                this.internReferanseKartverket = lenke.beskrivelse
            }
        }

        dokumentasjonsreferanse.vedtakGyldigFra = dokRef.vedtakGyldigFra ?: throw BadRequestException("\"Gyldig fra\"-dato er påkrevd!")
        dokumentasjonsreferanse.vedtakGyldigTil = dokRef.vedtakGyldigTil
        dokumentasjonsreferanse.fastsettingsdato = dokRef.fastsettingsdato
        dokumentasjonsreferanse.fastsettingsmyndighet = dokRef.fastsettingsmyndighet
        dokumentasjonsreferanse.hjemmel = dokRef.hjemmel
        dokumentasjonsreferanse.rettskildeid = dokRef.rettskildeId
        dokumentasjonsreferanse.rettskildetittel = dokRef.rettskildeTittel
    }

    private fun <T> validateReferanseId(referanse: T, id: String?, exceptionMessage: String) {
        if (referanse == null && id != null)
            throw BadRequestException(exceptionMessage)
    }

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
