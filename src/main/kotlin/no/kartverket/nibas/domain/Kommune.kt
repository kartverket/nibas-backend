package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.*

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "kommune")
@Audited
@Entity
class Kommune(override val id: String = UUID.randomUUID().toString()) :
    AdministrativEnhet(Flatetype.KOMMUNE) {

    @Column(name = "kommunenummer", nullable = false)
    var kommunenummer: String = ""

    @Column(name = "samiskforvaltningsomraade", nullable = false)
    var samiskforvaltningsomraade: Boolean? = false

    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    var fylkeFk: Fylke? = null

    @OneToMany(mappedBy = "kommune", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var kommunegrenser: MutableSet<KommuneKommunegrense> = mutableSetOf()

    @OneToMany(mappedBy = "kommune", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var fylkesgrenser: MutableSet<KommuneFylkesgrense> = mutableSetOf()

    @OneToMany(mappedBy = "kommune", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var avtaltavgrensningslinjer: MutableSet<KommuneAvtaltavgrensningslinje> = mutableSetOf()

    @OneToMany(mappedBy = "kommune", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var territorialgrenser: MutableSet<KommuneTerritorialgrense> = mutableSetOf()

    @OneToMany(mappedBy = "kommune", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var riksgrenser: MutableSet<KommuneRiksgrense> = mutableSetOf()

    @OneToMany(mappedBy = "kommuneFk", fetch = FetchType.LAZY)
    var informasjonselementer: MutableSet<KommuneInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "kommuneFk", fetch = FetchType.LAZY)
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { KommuneInformasjon(this) }
    }

    override fun oppdaterFlate() {
        flate = SosiGeometryFunctions.beregnFlate(kommunegrenser + fylkesgrenser + avtaltavgrensningslinjer + territorialgrenser + riksgrenser)
    }

    override fun oppdaterEllerLeggTilGrenseKobling(grense: Grense, attributter: GrenseKoblingAttributter) {
        val (samling, default) = when(grense) {
            is Kommunegrense -> SamlingOgDefault(kommunegrenser, KommuneKommunegrense(this, grense, attributter))
            is Fylkesgrense -> SamlingOgDefault(fylkesgrenser, KommuneFylkesgrense(this, grense, attributter))
            is Avtaltavgrensningslinje -> SamlingOgDefault(avtaltavgrensningslinjer, KommuneAvtaltavgrensningslinje(this, grense, attributter))
            is Territorialgrense -> SamlingOgDefault(territorialgrenser, KommuneTerritorialgrense(this, grense, attributter))
            is Riksgrense -> SamlingOgDefault(riksgrenser, KommuneRiksgrense(this, grense, attributter))
            else -> throw RuntimeException("Kan ikke legge til ${grense::class.simpleName} på enhet Kommune")
        }
        oppdaterSamling(grense, attributter, samling, default)
    }

}
