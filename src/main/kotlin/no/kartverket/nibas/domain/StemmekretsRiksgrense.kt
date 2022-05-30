package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "stemmekrets_riksgrense")
@Audited
@Entity
@IdClass(StemmekretsRiksgrenseId::class)
class StemmekretsRiksgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "stemmekrets_fk")
    var stemmekrets: Stemmekrets,

    @Id
    @ManyToOne
    @JoinColumn(name = "riksgrense_fk")
    var riksgrense: Riksgrense,

    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter

) : GrenseKobling {
    override fun getGrense(): Grense = riksgrense
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }
}
data class StemmekretsRiksgrenseId (val stemmekrets: String = "", val riksgrense: String = "") : Serializable
