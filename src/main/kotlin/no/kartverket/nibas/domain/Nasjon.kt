package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.*

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "nasjon")
@Audited
@Entity
class Nasjon(override val id: String = UUID.randomUUID().toString()) :
    AdministrativEnhet(AdministrativEnhetType.NASJON) {

    @OneToMany(mappedBy = "nasjonFk", fetch = FetchType.LAZY)
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    @OneToMany(mappedBy = "nasjon", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var avtaltavgrensningslinjer: MutableSet<NasjonAvtaltavgrensningslinje> = mutableSetOf()

    @OneToMany(mappedBy = "nasjon", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var territorialgrenser: MutableSet<NasjonTerritorialgrense> = mutableSetOf()

    @OneToMany(mappedBy = "nasjon", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var riksgrenser: MutableSet<NasjonRiksgrense> = mutableSetOf()

    @OneToMany(mappedBy = "nasjonFk", fetch = FetchType.LAZY)
    var informasjonselementer: MutableSet<NasjonInformasjon> = mutableSetOf()

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { NasjonInformasjon(this) }
    }

    override fun oppdaterFlate() {
        flate = SosiGeometryFunctions.beregnFlate(avtaltavgrensningslinjer + territorialgrenser + riksgrenser)
    }

    override fun oppdaterEllerLeggTilGrenseKobling(grense: Grense, attributter: GrenseKoblingAttributter) {
        val (samling, default) = when(grense) {
            is Avtaltavgrensningslinje -> SamlingOgDefault(avtaltavgrensningslinjer, NasjonAvtaltavgrensningslinje(this, grense, attributter))
            is Territorialgrense -> SamlingOgDefault(territorialgrenser, NasjonTerritorialgrense(this, grense, attributter))
            is Riksgrense -> SamlingOgDefault(riksgrenser, NasjonRiksgrense(this, grense, attributter))
            else -> throw RuntimeException("Kan ikke legge til ${grense::class.simpleName} på enhet Nasjon")
        }
        oppdaterSamling(grense, attributter, samling, default)
    }
}
