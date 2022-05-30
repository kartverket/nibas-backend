package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "stemmekrets_fylkesgrense")
@Audited
@Entity
@IdClass(StemmekretsFylkesgrenseId::class)
class StemmekretsFylkesgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "stemmekrets_fk")
    var stemmekrets: Stemmekrets,

    @Id
    @ManyToOne
    @JoinColumn(name = "fylkesgrense_fk")
    var fylkesgrense: Fylkesgrense,

    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter

) : GrenseKobling {

    override fun getGrense(): Grense = fylkesgrense
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }
}
data class StemmekretsFylkesgrenseId (val stemmekrets: String = "", val fylkesgrense: String = "") : Serializable
