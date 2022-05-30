package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "stemmekrets_territorialgrense")
@Audited
@Entity
@IdClass(StemmekretsTerritorialgrenseId::class)
class StemmekretsTerritorialgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "stemmekrets_fk")
    var stemmekrets: Stemmekrets,

    @Id
    @ManyToOne
    @JoinColumn(name = "territorialgrense_fk")
    var territorialgrense: Territorialgrense,

    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter

) : GrenseKobling {
    override fun getGrense(): Grense = territorialgrense
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }
}
data class StemmekretsTerritorialgrenseId (val stemmekrets: String = "", val territorialgrense: String = "") : Serializable
