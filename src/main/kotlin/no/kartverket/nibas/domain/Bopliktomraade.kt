package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.geometry.toMultiPolygon
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import org.hibernate.annotations.SQLRestriction
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.io.WKTWriter
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.time.LocalDate
import java.util.*
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import jakarta.persistence.Table

enum class MaterieltVilkaar {
    BEBYGDEIENDOM,
    IKKEHELAARSBOLIGUNDEROPPFORING,
    UBEBYGDTOMT,
    UNNTAKFRASLEKTSKAPSUNNTAK
}

// Nye verdier for materielle vilkår, navngitt etter de nye db-feltene. Eksponeres via feltet
// gjeldendeMaterielleVilkaar, mens MaterieltVilkaar beholdes uendret for bakoverkompatibilitet.
enum class GjeldendeMaterieltVilkaar {
    GJELDER_FOR_BOLIG_IKKE_TATT_I_BRUK,
    GJELDER_FOR_BRUKT_SOM_HELARSBOLIG,
    GJELDER_FOR_UBEBYGD_BOLIGTOMT,
    HAR_UNNTAK_FRA_SLEKTSKAPSUNNTAK
}

@Table(name = "bopliktomraade")
@Entity
class Bopliktomraade(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString()
) : InndelingMedDeltGeometri(Flatetype.BOPLIKTOMRAADE), KobletTilGrense {

    @Column(name = "navn", nullable = false)
    lateinit var navn: String

    @Column(name = "nummer", nullable = false)
    lateinit var nummer: String

    @Column(name = "informasjon")
    override var informasjon: String? = null

    @Column(name = "usikker_avgrensning", nullable = false)
    var usikkerAvgrensning: Boolean = false

    @Column(name = "har_usikker_avgrensning", nullable = false)
    var harUsikkerAvgrensning: Boolean = false

    @Column(name = "forskriftsreferanse")
    var forskriftsreferanse: String? = null

    @Column(name = "url")
    var url: String? = null

    @Column(name = "bebygd_eiendom")
    var bebygdEiendom: Boolean = false

    @Column(name = "gjelder_for_bolig_ikke_tatt_i_bruk")
    var gjelderForBoligIkkeTattIBruk: Boolean = false

    @Column(name = "ikke_helaarsbolig_under_oppforing")
    var ikkeHelaarsBoligUnderOppforing: Boolean = false

    @Column(name = "gjelder_for_brukt_som_helarsbolig")
    var gjelderForBruktSomHelarsbolig: Boolean = false

    @Column(name = "ubebygd_tomt")
    var ubebygdTomt: Boolean = false

    @Column(name = "gjelder_for_ubebygd_boligtomt")
    var gjelderForUbebygdBoligtomt: Boolean = false

    @Column(name = "unntak_fra_slektskapsunntak")
    var unntakFraSlektskapsunntak: Boolean = false

    @Column(name = "har_unntak_fra_slektskapsunntak")
    var harUnntakFraSlektskapsunntak: Boolean = false

    @Column(name = "andre_avgrensninger")
    var andreAvgrensninger: String? = null

    @Column(name = "andre_lokale_avgrensninger")
    var andreLokaleAvgrensninger: String? = null

    val materielleVilkaar: Set<MaterieltVilkaar>
        get() = buildSet {
            if (bebygdEiendom) add(MaterieltVilkaar.BEBYGDEIENDOM)
            if (ikkeHelaarsBoligUnderOppforing) add(MaterieltVilkaar.IKKEHELAARSBOLIGUNDEROPPFORING)
            if (ubebygdTomt) add(MaterieltVilkaar.UBEBYGDTOMT)
            if (unntakFraSlektskapsunntak) add(MaterieltVilkaar.UNNTAKFRASLEKTSKAPSUNNTAK)
        }

    val gjeldendeMaterielleVilkaar: Set<GjeldendeMaterieltVilkaar>
        get() = buildSet {
            if (gjelderForBoligIkkeTattIBruk) add(GjeldendeMaterieltVilkaar.GJELDER_FOR_BOLIG_IKKE_TATT_I_BRUK)
            if (gjelderForBruktSomHelarsbolig) add(GjeldendeMaterieltVilkaar.GJELDER_FOR_BRUKT_SOM_HELARSBOLIG)
            if (gjelderForUbebygdBoligtomt) add(GjeldendeMaterieltVilkaar.GJELDER_FOR_UBEBYGD_BOLIGTOMT)
            if (harUnntakFraSlektskapsunntak) add(GjeldendeMaterieltVilkaar.HAR_UNNTAK_FRA_SLEKTSKAPSUNNTAK)
        }

    fun setMaterielleVilkaar(vilkaar: Set<MaterieltVilkaar>) {
        val bebygd = MaterieltVilkaar.BEBYGDEIENDOM in vilkaar
        val ikkeHelaars = MaterieltVilkaar.IKKEHELAARSBOLIGUNDEROPPFORING in vilkaar
        val ubebygd = MaterieltVilkaar.UBEBYGDTOMT in vilkaar
        val unntak = MaterieltVilkaar.UNNTAKFRASLEKTSKAPSUNNTAK in vilkaar
        bebygdEiendom = bebygd
        gjelderForBoligIkkeTattIBruk = bebygd
        ikkeHelaarsBoligUnderOppforing = ikkeHelaars
        gjelderForBruktSomHelarsbolig = ikkeHelaars
        ubebygdTomt = ubebygd
        gjelderForUbebygdBoligtomt = ubebygd
        unntakFraSlektskapsunntak = unntak
        harUnntakFraSlektskapsunntak = unntak
    }

    fun setGjeldendeMaterielleVilkaar(vilkaar: Set<GjeldendeMaterieltVilkaar>) {
        val boligIkkeTattIBruk = GjeldendeMaterieltVilkaar.GJELDER_FOR_BOLIG_IKKE_TATT_I_BRUK in vilkaar
        val bruktSomHelarsbolig = GjeldendeMaterieltVilkaar.GJELDER_FOR_BRUKT_SOM_HELARSBOLIG in vilkaar
        val ubebygdBoligtomt = GjeldendeMaterieltVilkaar.GJELDER_FOR_UBEBYGD_BOLIGTOMT in vilkaar
        val unntak = GjeldendeMaterieltVilkaar.HAR_UNNTAK_FRA_SLEKTSKAPSUNNTAK in vilkaar
        gjelderForBoligIkkeTattIBruk = boligIkkeTattIBruk
        bebygdEiendom = boligIkkeTattIBruk
        gjelderForBruktSomHelarsbolig = bruktSomHelarsbolig
        ikkeHelaarsBoligUnderOppforing = bruktSomHelarsbolig
        gjelderForUbebygdBoligtomt = ubebygdBoligtomt
        ubebygdTomt = ubebygdBoligtomt
        harUnntakFraSlektskapsunntak = unntak
        unntakFraSlektskapsunntak = unntak
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kommunelokalid", referencedColumnName = "lokalid")
    @SQLRestriction("flatetype='KOMMUNE'")
    lateinit var kommuneLokalid: UnikLokalid

    fun getKommune(gyldighetsdato: LocalDate = LocalDate.now()): Kommune? {
        return kommuneLokalid.kommuner.firstOrNull { it.gyldigPaaDato(gyldighetsdato) }
    }

    override var omraade: MultiPolygon?
        get() = super.omraade
        set(value) {
            if (value != null && !delvisBoplikt) {
                bopliktomraadeGrenserInternal = BopliktomraadeGrenser.DelvisBoplikt()
            } else if (value == null && delvisBoplikt) {
                bopliktomraadeGrenserInternal = BopliktomraadeGrenser.FullBoplikt()
            }
            super.omraade = value
        }

    // Dette er workaround for at hibernate initialserer instanser av denne klassen
    // der det er nullverdier, fordi den kan finne på å skippe initialisering av feltet
    @Embedded
    @JvmField
    final var bopliktomraadeGrenserInternal: BopliktomraadeGrenser? = null

    var bopliktomraadeGrenser: BopliktomraadeGrenser
        get() = when (val value = bopliktomraadeGrenserInternal) {
            null -> BopliktomraadeGrenser.FullBoplikt().also {
                bopliktomraadeGrenserInternal = it
            }
            else -> value
        }
        set(value) {
            if (value is BopliktomraadeGrenser.FullBoplikt) {
                super.omraade = null
            }
            bopliktomraadeGrenserInternal = value
        }

    val delvisBoplikt: Boolean get() = when (bopliktomraadeGrenser) {
        is BopliktomraadeGrenser.DelvisBoplikt -> true
        is BopliktomraadeGrenser.FullBoplikt -> false
    }

    val gjelderKunDelAvKommunen: Boolean get() = when (bopliktomraadeGrenser) {
        is BopliktomraadeGrenser.DelvisBoplikt -> true
        is BopliktomraadeGrenser.FullBoplikt -> false
    }

    override fun getGrensekoblinger(gyldighetsdato: LocalDate): Set<BopliktomraadeGrenseKobling> {
        return when(val geometry = bopliktomraadeGrenser) {
            is BopliktomraadeGrenser.DelvisBoplikt -> geometry.getGrensekoblinger(gyldighetsdato)
            is BopliktomraadeGrenser.FullBoplikt -> getKommune(gyldighetsdato)
                ?.getGrensekoblinger(gyldighetsdato)
                ?.map { BopliktomraadeGrenseKobling(this, it.grense) }
                ?.toSet()
                ?: emptySet()
        }
    }

    override fun addGrensekobling(grense: Grense) {
        when(val geometry = bopliktomraadeGrenser) {
            is BopliktomraadeGrenser.DelvisBoplikt -> geometry.addGrensekobling(this, grense)
            is BopliktomraadeGrenser.FullBoplikt -> throw IllegalStateException("Kan ikke legge til grense på Bopliktomraade med full boplikt")
        }
    }

    override fun removeGrensekobling(gammelGrense: Grense) {
        when(val geometry = bopliktomraadeGrenser) {
            is BopliktomraadeGrenser.DelvisBoplikt -> geometry.removeGrensekobling(this, gammelGrense)
            is BopliktomraadeGrenser.FullBoplikt -> throw IllegalStateException("Kan ikke fjerne grense på Bopliktomraade med full boplikt")
        }
    }

    override fun replaceGrensekobling(gammelGrense: Grense, grense: Grense?) {
        when(val geometry = bopliktomraadeGrenser) {
            is BopliktomraadeGrenser.DelvisBoplikt -> geometry.replaceGrensekobling(this, gammelGrense, grense)
            is BopliktomraadeGrenser.FullBoplikt -> throw IllegalStateException("Kan ikke oppdatere/legge til grense på Bopliktomraade med full boplikt")
        }
    }

    override fun oppdaterOmraadeOgPosisjon() {
        when (val geometry = bopliktomraadeGrenser) {
            is BopliktomraadeGrenser.DelvisBoplikt -> {
                omraade = getGrenserIRiktigRekkefoelge(geometry
                    .grenser
                    .filter { it.grense.gyldighet.overlapperMed(gyldighet) }
                    .map { it.grense.grense }
                ).toMultiPolygon()
                posisjon = omraade?.interiorPoint
            }

            is BopliktomraadeGrenser.FullBoplikt -> {
                omraade = null
                posisjon = kommuneLokalid.kommuner
                    .asSequence()
                    .filter { this.gyldighet.overlapperMed(it.gyldighet) }
                    .sortedWith(Comparator
                        .comparing<Kommune, LocalDate> { it.gyldighet.gyldigTil ?: LocalDate.MAX }
                        .thenBy { it.gyldighet.gyldigFra }
                        .reversed()
                    )
                    .firstOrNull()
                    ?.posisjon
            }
        }
    }

    override fun getFlateInformasjon(): FlateInformasjon =
        FlateInformasjon(type, "$nummer $navn")

    override fun toString(): String =
        "Bopliktomraade(id='$id', gyldighet: $gyldighet)"

    override fun toEventRad(eventType: EventType): EventRad =
        EventRad(
            uuid = this.id,
            eventType = eventType,
            flateType = Flatetype.BOPLIKTOMRAADE,
            lokalid = this.lokalid
        )

    fun applyMappingFromExistingBopliktomraade(gammeltBopliktomraade: Bopliktomraade): Bopliktomraade =
        this.apply {
            nummer = gammeltBopliktomraade.nummer
            navn = gammeltBopliktomraade.navn
            omraade = gammeltBopliktomraade.omraade
            usikkerAvgrensning = gammeltBopliktomraade.usikkerAvgrensning
            harUsikkerAvgrensning = gammeltBopliktomraade.harUsikkerAvgrensning
            bopliktomraadeGrenser = when (gammeltBopliktomraade.bopliktomraadeGrenser) {
                is BopliktomraadeGrenser.DelvisBoplikt -> BopliktomraadeGrenser.DelvisBoplikt()
                is BopliktomraadeGrenser.FullBoplikt -> BopliktomraadeGrenser.FullBoplikt()
            }
            bebygdEiendom = gammeltBopliktomraade.bebygdEiendom
            gjelderForBoligIkkeTattIBruk = gammeltBopliktomraade.gjelderForBoligIkkeTattIBruk
            ikkeHelaarsBoligUnderOppforing = gammeltBopliktomraade.ikkeHelaarsBoligUnderOppforing
            gjelderForBruktSomHelarsbolig = gammeltBopliktomraade.gjelderForBruktSomHelarsbolig
            ubebygdTomt = gammeltBopliktomraade.ubebygdTomt
            gjelderForUbebygdBoligtomt = gammeltBopliktomraade.gjelderForUbebygdBoligtomt
            unntakFraSlektskapsunntak = gammeltBopliktomraade.unntakFraSlektskapsunntak
            harUnntakFraSlektskapsunntak = gammeltBopliktomraade.harUnntakFraSlektskapsunntak
            andreAvgrensninger = gammeltBopliktomraade.andreAvgrensninger
            andreLokaleAvgrensninger = gammeltBopliktomraade.andreLokaleAvgrensninger
            forskriftsreferanse = gammeltBopliktomraade.forskriftsreferanse
            url = gammeltBopliktomraade.url
            datafangstdato = gammeltBopliktomraade.datafangstdato
            foerstedigitaliseringsdato = gammeltBopliktomraade.foerstedigitaliseringsdato
            posisjon = gammeltBopliktomraade.posisjon
            opphav = gammeltBopliktomraade.opphav
            informasjon = gammeltBopliktomraade.informasjon
        }

    fun copyGrenserFromExistingBopliktomraade(gammeltBopliktomraade: Bopliktomraade, gyldigFra: LocalDate = LocalDate.now()): Bopliktomraade {
        gammeltBopliktomraade.getGrensekoblinger(gyldigFra).forEach { gammelGrenseKobling ->
            addGrensekobling(grense = gammelGrenseKobling.grense)
        }
        return this
    }


    /**
    * Sjekker om bopliktområdet ligger innenfor kommunen, eller om de ikke overlapper i tid.
    *
    * Returnerer `true` hvis:
    * - Bopliktområdet og kommunen har ulike gyldighetsperioder som ikke overlapper
    * - Bopliktområdet ligger innenfor kommunens grenser (med en liten buffer på [KOMMUNE_BOUNDARY_BUFFER_METER])
    * - Enten bopliktområdet eller kommunen mangler geometri
    *
    * @param kommune kommunen som skal sjekkes mot
    * @return `true` hvis området ligger innenfor kommunen eller ikke overlapper i tid, `false` ellers
    */
    fun isCoveredByOrTemporallyDisjoint(kommune: Kommune): Boolean {
        if (!gyldighet.overlapperMed(kommune.gyldighet)) {
            return true
        }
        val kommuneOmraade = kommune.omraade?.takeUnless { it.isEmpty } ?: return true
        val thisOmraade = omraade?.takeUnless { it.isEmpty } ?: return true
        val coveredByKommune = kommuneOmraade
            .buffer(KOMMUNE_BOUNDARY_BUFFER_METER)
            .covers(thisOmraade)
        if (!coveredByKommune) {
            val differenceText = WKTWriter()
                .writeFormatted(kommuneOmraade.difference(thisOmraade))
                .let {
                    if (it.length > 500) {
                        it.substring(0, 500) + "..."
                    } else {
                        it
                    }
                }
            log.error(
                "Bopliktomraade({}) er ikke dekket av kommune {}({})@{}:\n{}",
                id, kommune.kommunenummer, kommune.id, kommune.gyldighet, differenceText
            )
            return false
        }
        return true
    }

    @PostUpdate
    @PostPersist
    fun validateCoveredByKommune() {
        if (!this::kommuneLokalid.isInitialized) return
        val valid = kommuneLokalid.kommuner.asSequence()
            .map(this::isCoveredByOrTemporallyDisjoint)
            .fold(true) { a, b -> a && b }
        require(valid) {
            "Bopliktomraade($id) er ikke dekket av kommunen(i hele sin gyldighetsperiode: $gyldighet)"
        }
    }

    companion object {
        const val KOMMUNE_BOUNDARY_BUFFER_METER = 1.0 / 1_000_000.0
        private val log = LoggerFactory.getLogger(Bopliktomraade::class.java)
        fun tillatteGrensetyper(): Set<Grensetype> = setOf(
            Grensetype.Bopliktgrense
        )
    }
}

/**
 * Koblingstabell mellom Bopliktomraade - Grense
 */
@Table(name = "bopliktomraade_grense")
@Entity
@IdClass(BopliktomraadeGrenseId::class)
class BopliktomraadeGrenseKobling(
    @Id
    @ManyToOne
    @JoinColumn(name = "bopliktomraade_fk")
    var bopliktomraade: Bopliktomraade,

    @Id
    @ManyToOne
    @JoinColumn(name = "grense_fk")
    override var grense: Grense,
) : GrenseKobling {
    override fun getEnhetKobletTilGrense(): KobletTilGrense {
        return bopliktomraade
    }

    override fun getLokalIdForKrets(): String {
        return bopliktomraade.lokalid
    }
}

data class BopliktomraadeGrenseId(val bopliktomraade: String = "", val grense: String = "") : Serializable
