package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "stemmekrets")
class Stemmekrets(override val id: String = UUID.randomUUID().toString()) : KommunalKrets(Flatetype.STEMMEKRETS) {

    override var objtype: String? = Flatetype.STEMMEKRETS.name

    @Column(name = "stemmekretsnummer", nullable = false)
    var stemmekretsnummer: String? = null

    @Column(name = "stemmekretsnavn", nullable = false)
    var stemmekretsnavn: String? = null

    @Column(name = "tellekretsnummer")
    var tellekretsnummer: String? = null

    @Column(name = "tellekretsnavn")
    var tellekretsnavn: String? = null

    @Column(name = "valgdistriktsnummer")
    var valgdistriktsnummer: String? = null

    @Column(name = "fylkesnummer")
    var fylkesnummer: String? = null

    @Column(name = "kommunenummer", nullable = false)
    var kommunenummer: String? = null

    @OneToMany(mappedBy = "stemmekrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var fylkesgrenser: MutableSet<StemmekretsFylkesgrense> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "stemmekrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var avtaltavgrensningslinjer: MutableSet<StemmekretsAvtaltavgrensningslinje> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "stemmekrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var kommunegrenser: MutableSet<StemmekretsKommunegrense> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "stemmekrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var riksgrenser: MutableSet<StemmekretsRiksgrense> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "stemmekrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var stemmekretsgrenser: MutableSet<StemmekretsStemmekretsgrense> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "stemmekrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var territorialgrenser: MutableSet<StemmekretsTerritorialgrense> = Collections.synchronizedSet(mutableSetOf())

    override fun oppdaterFlate() {
        flate = SosiGeometryFunctions.beregnFlate(alleGrenser())
    }

    override fun oppdaterEllerLeggTilGrenseKobling(grense: Grense, attributter: GrenseKoblingAttributter) {
        val (samling, default) = when(grense) {
            is Stemmekretsgrense -> SamlingOgDefault(stemmekretsgrenser, StemmekretsStemmekretsgrense(this, grense, attributter.retningMedKlokken, attributter.rekkefoelge, attributter.flateIndeks, attributter.hullIndeks))
            is Kommunegrense -> SamlingOgDefault(kommunegrenser, StemmekretsKommunegrense(this, grense, attributter))
            is Fylkesgrense -> SamlingOgDefault(fylkesgrenser, StemmekretsFylkesgrense(this, grense, attributter))
            is Avtaltavgrensningslinje -> SamlingOgDefault(avtaltavgrensningslinjer, StemmekretsAvtaltavgrensningslinje(this, grense, attributter))
            is Territorialgrense -> SamlingOgDefault(territorialgrenser, StemmekretsTerritorialgrense(this, grense, attributter))
            is Riksgrense -> SamlingOgDefault(riksgrenser, StemmekretsRiksgrense(this, grense, attributter))
            else -> throw RuntimeException("Kan ikke legge til ${grense::class.simpleName} på enhet Fylke")
        }

        oppdaterSamling(grense, attributter, samling, default)
    }

    override fun getInfo(): String? = informasjon
    override fun setInfo(updatedInfo: String?) {
        informasjon = updatedInfo
    }

    override fun toString(): String {
        return "Stemmekrets(id='$id', stemmekretsnummer=$stemmekretsnummer, stemmekretsnavn=$stemmekretsnavn, tellekretsnummer=$tellekretsnummer, tellekretsnavn=$tellekretsnavn, valgdistriktsnummer=$valgdistriktsnummer, fylkesnummer=$fylkesnummer, kommunenummer=$kommunenummer)"
    }

    fun alleGrenser() = stemmekretsgrenser + kommunegrenser + fylkesgrenser + avtaltavgrensningslinjer + territorialgrenser + riksgrenser
}
