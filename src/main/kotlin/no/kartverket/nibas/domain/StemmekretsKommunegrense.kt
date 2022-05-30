package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "stemmekrets_kommunegrense")
@Audited
@Entity
@IdClass(StemmekretsKommunegrenseId::class)
class StemmekretsKommunegrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "stemmekrets_fk")
    var stemmekrets: Stemmekrets,

    @Id
    @ManyToOne
    @JoinColumn(name = "kommunegrense_fk")
    var kommunegrense: Kommunegrense,

    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter

) : GrenseKobling {
    override fun getGrense(): Grense = kommunegrense
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }
}
data class StemmekretsKommunegrenseId (val stemmekrets: String = "", val kommunegrense: String = "") : Serializable
