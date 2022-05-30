package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "stemmekrets_stemmekretsgrense")
@Audited
@Entity
@IdClass(StemmekretsStemmekretsgrenseId::class)
class StemmekretsStemmekretsgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "stemmekrets_fk")
    var stemmekrets: Stemmekrets,

    @Id
    @ManyToOne
    @JoinColumn(name = "stemmekretsgrense_fk")
    var stemmekretsgrense: Stemmekretsgrense,

    @Column(name = "retning_med_klokken", nullable = false)
    var retningMedKlokken: Boolean = true,

    @Column(name = "rekkefoelge", nullable = false)
    var rekkefoelge: Int = 0,

    @Id
    @Column(name = "flate_indeks", nullable = false)
    var flateIndeks: Int = 0,

    @Column(name = "hull_indeks", nullable = true)
    var hullIndeks: Int? = null

) : GrenseKobling {
    override var grenseKoblingAttributter: GrenseKoblingAttributter
        get() = GrenseKoblingAttributter(retningMedKlokken, rekkefoelge, flateIndeks, hullIndeks)
        set(value) {
            retningMedKlokken = value.retningMedKlokken
            rekkefoelge = value.rekkefoelge
            flateIndeks = value.flateIndeks
            hullIndeks = value.hullIndeks
        }

    override fun getGrense(): Grense = stemmekretsgrense
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }

}
data class StemmekretsStemmekretsgrenseId (val stemmekrets: String = "", val stemmekretsgrense: String = "", val flateIndeks: Int = 0) : Serializable
