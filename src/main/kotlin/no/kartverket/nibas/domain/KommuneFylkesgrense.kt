package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "kommune_fylkesgrense")
@Audited
@Entity
@IdClass(KommuneFylkesgrenseId::class)
class KommuneFylkesgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    var kommune: Kommune,

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

data class KommuneFylkesgrenseId (val kommune: String = "", val fylkesgrense: String = "") : Serializable