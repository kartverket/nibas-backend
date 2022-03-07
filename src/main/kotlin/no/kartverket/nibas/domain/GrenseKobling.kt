package no.kartverket.nibas.domain

import javax.persistence.Column
import javax.persistence.Embeddable

interface GrenseKobling {
    var grenseKoblingAttributter: GrenseKoblingAttributter

    fun getGrense(): Grense
    fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter)
}

@Embeddable
data class GrenseKoblingAttributter(
    @Column(name = "retning_med_klokken", nullable = false)
    var retningMedKlokken: Boolean = true,

    @Column(name = "rekkefoelge", nullable = false)
    var rekkefoelge: Int = 0,

    @Column(name = "flate_indeks", nullable = false)
    var flateIndeks: Int = 0,

    @Column(name = "hull_indeks", nullable = true)
    var hullIndeks: Int? = null

) {
    fun oppdater(grenseKoblingAttributter: GrenseKoblingAttributter): GrenseKoblingAttributter {
        return this.copy(
            retningMedKlokken = grenseKoblingAttributter.retningMedKlokken,
            rekkefoelge = grenseKoblingAttributter.rekkefoelge,
            hullIndeks = grenseKoblingAttributter.hullIndeks
        )
    }
}