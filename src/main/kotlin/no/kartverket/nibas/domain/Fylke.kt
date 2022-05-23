package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.*

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "fylke")
@Audited
@Entity
class Fylke(override val id: String = UUID.randomUUID().toString()) :
    AdministrativEnhet(Flatetype.FYLKE) {

    @Column(name = "fylkesnummer", nullable = false)
    var fylkesnummer: String = ""

    @Column(name = "samiskforvaltningsomraade")
    var samiskforvaltningsomraade: Boolean? = null

    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjonFk: Nasjon? = null

    @OneToMany(mappedBy = "fylke", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var fylkesgrenser: MutableSet<FylkeFylkesgrense> = mutableSetOf()

    @OneToMany(mappedBy = "fylke", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var avtaltavgrensningslinjer: MutableSet<FylkeAvtaltavgrensningslinje> = mutableSetOf()

    @OneToMany(mappedBy = "fylke", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var territorialgrenser: MutableSet<FylkeTerritorialgrense> = mutableSetOf()

    @OneToMany(mappedBy = "fylke", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var riksgrenser: MutableSet<FylkeRiksgrense> = mutableSetOf()

    @OneToMany(mappedBy = "fylkeFk", fetch = FetchType.LAZY)
    var informasjonselementer: MutableSet<FylkeInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "fylkeFk", fetch = FetchType.LAZY)
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { FylkeInformasjon(this) }
    }

    override fun oppdaterFlate() {
        flate = SosiGeometryFunctions.beregnFlate(fylkesgrenser + avtaltavgrensningslinjer + territorialgrenser + riksgrenser)
    }

    override fun oppdaterEllerLeggTilGrenseKobling(grense: Grense, attributter: GrenseKoblingAttributter) {
        val (samling, default) = when(grense) {
            is Fylkesgrense -> SamlingOgDefault(fylkesgrenser, FylkeFylkesgrense(this, grense, attributter))
            is Avtaltavgrensningslinje -> SamlingOgDefault(avtaltavgrensningslinjer, FylkeAvtaltavgrensningslinje(this, grense, attributter))
            is Territorialgrense -> SamlingOgDefault(territorialgrenser, FylkeTerritorialgrense(this, grense, attributter))
            is Riksgrense -> SamlingOgDefault(riksgrenser, FylkeRiksgrense(this, grense, attributter))
            else -> throw RuntimeException("Kan ikke legge til ${grense::class.simpleName} på enhet Fylke")
        }

        oppdaterSamling(grense, attributter, samling, default)
    }
}
