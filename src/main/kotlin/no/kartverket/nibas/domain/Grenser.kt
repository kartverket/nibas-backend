package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import org.locationtech.jts.geom.LineString
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
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
    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], mappedBy = "grense")
    var dokrefGrensekobling: MutableSet<DokrefGrenseKobling> = Collections.synchronizedSet(mutableSetOf())

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @Embedded
    override var gyldighet: Gyldighet = Gyldighet()

    @Embedded
    var kvalitet: PosisjonskvalitetEmbeddable? = PosisjonskvalitetEmbeddable()

    @Version
    override var version: Int = 1

    @Column(name = "datafangstdato")
    override var datafangstdato: ZonedDateTime? = null

    @Column(name = "foerstedigitaliseringsdato", nullable = false, updatable = false)
    override val foerstedigitaliseringsdato: ZonedDateTime = ZonedDateTime.now()

    @Column(name = "opphav")
    override var opphav: String? = null

    @Column(name = "grense", nullable = false)
    lateinit var grense: LineString

    @Column(name = "informasjon")
    override var informasjon: String? = null

    // Relasjon oppover i treet til Grunnkrets
    @OneToMany(mappedBy = "grense")
    var grunnkretser: MutableSet<GrunnkretsGrenseKobling> = mutableSetOf()

    // Relasjon oppover i treet til Stemmekrets
    @OneToMany(mappedBy = "grense")
    var stemmekretser: MutableSet<StemmekretsGrenseKobling> = mutableSetOf()

    @OneToMany(mappedBy = "grense")
    var bopliktomraader: MutableSet<BopliktomraadeGrenseKobling> = mutableSetOf()

    fun getAllDokumentasjonsreferanser() = dokrefGrensekobling.map { it.dokumentasjonsreferanse }.toSet()
    fun getDokumentasjonsreferanser(gyldighetsdato: LocalDate = LocalDate.now()) = dokrefGrensekobling.map { it.dokumentasjonsreferanse }.filter { it.gyldigPaaDato(gyldighetsdato) }.toSet()
    fun grunnkretser(gyldighetsdato: LocalDate = LocalDate.now()): Set<Grunnkrets> =
        grunnkretser.map { it.grunnkrets }.filter { it.gyldigPaaDato(gyldighetsdato) }.toSet()

    fun gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato: LocalDate = LocalDate.now()): Set<GrunnkretsGrenseKobling> =
        grunnkretser.filter { it.grunnkrets.gyldigPaaDato(gyldighetsdato) }.toSet()

    fun stemmekretser(gyldighetsdato: LocalDate = LocalDate.now()): Set<Stemmekrets> =
        stemmekretser.map { it.stemmekrets }.filter { it.gyldigPaaDato(gyldighetsdato) }.toSet()

    fun gyldigeStemmekretsGrenseKoblinger(gyldighetsdato: LocalDate = LocalDate.now()): Set<StemmekretsGrenseKobling> =
        stemmekretser.filter { it.stemmekrets.gyldigPaaDato(gyldighetsdato) }.toSet()

    fun gyldigeBopliktomraadeGrenseKoblinger(gyldighetsdato: LocalDate = LocalDate.now()): Set<BopliktomraadeGrenseKobling> =
        bopliktomraader.filter { it.bopliktomraade.gyldigPaaDato(gyldighetsdato) }.toSet()

    /**
     * Returnerer flater fra ikke-heldekkende inndelinger som er koblet til denne grensen på gitt dato.
     *
     * Koden er skrevet per ikke-heldekkende grensetype slik at "hvordan finner jeg tilkoblede flater for en gitt
     * ikke-heldekkende grensetype" er dokumentert i kode. Nye [NonExhaustiveGrensetype]-verdier tvinger
     * en oppdatering av dette when-uttrykket i kompilatoren.
     */
    fun gyldigeNonExhaustiveInndelinger(type: NonExhaustiveGrensetype, gyldighetsdato: LocalDate = LocalDate.now()): Sequence<Flate> =
        when (type) {
            NonExhaustiveGrensetype.Bopliktgrense ->
                gyldigeBopliktomraadeGrenseKoblinger(gyldighetsdato).asSequence().map { it.bopliktomraade }
        }

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
        return Grensetype.erAdministrativGrense(this.grensetype)
    }

    fun harGrenseTypeContainedInSet(grensetyper: Set<Grensetype>): Boolean = grensetyper.contains(this.grensetype)

    fun applyMappingFromExistingGrense(gammelGrense: Grense): Grense {
        return this.apply {
            gammelGrense.gyldighet.gyldigTil?.let { setGyldigTil(it) }
            datafangstdato = gammelGrense.datafangstdato
            opphav = gammelGrense.opphav
            grensetype = gammelGrense.grensetype
            kvalitet = gammelGrense.kvalitet
            informasjon = gammelGrense.informasjon
            grense = gammelGrense.grense
        }
    }

    fun applyCascadingOperationsBasedOnExistingGrense(gammelGrense: Grense): Grense {
        return this.apply {
            gammelGrense.dokrefGrensekobling.forEach { kobling ->
                if (kobling.grense != this) {
                    val nyKobling = DokrefGrenseKobling()

                    nyKobling.grense = this
                    nyKobling.dokumentasjonsreferanse = kobling.dokumentasjonsreferanse

                    dokrefGrensekobling.add(nyKobling)
                }
            }
        }
    }

    override fun toString(): String =
        "Grense(" +
            "id='$id', lokalid='$lokalid', grensetype=$grensetype, " +
            "auditInfo=$auditInfo, gyldighet=$gyldighet, kvalitet=$kvalitet, " +
            "version=$version, datafangstdato=$datafangstdato, foerstedigitaliseringsdato=$foerstedigitaliseringsdato, " +
            "opphav=$opphav, informasjon=$informasjon)"
}

/**
 * Undermengde av [Grensetype] for grenser som tilhører en inndeling som ikke er heldekkende
 * (`Flatetype.isExhaustive == false`).
 */
enum class NonExhaustiveGrensetype {
    Bopliktgrense
}

/**
 * Klassifiserer en [Grensetype] som ikke-heldekkende (returnerer [NonExhaustiveGrensetype]) eller `null`
 * for grenser som tilhører heldekkende inndelinger.
 *
 * Alle heldekkende grensetyper listes eksplisitt slik at kompilatoren tvinger klassifisering av nye
 * grensetyper her.
 */
fun Grensetype.asNonExhaustive(): NonExhaustiveGrensetype? = when (this) {
    Grensetype.Bopliktomraadegrense -> NonExhaustiveGrensetype.Bopliktgrense
    Grensetype.Kommunegrense,
    Grensetype.Fylkesgrense,
    Grensetype.Riksgrense,
    Grensetype.AvtaltAvgrensningslinje,
    Grensetype.Territorialgrense,
    Grensetype.Grunnlinje,
    Grensetype.Grunnkretsgrense,
    Grensetype.Delomraadegrense,
    Grensetype.Stemmekretsgrense -> null
}
