package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "kommune_territorialgrense")
@Audited
@Entity
@IdClass(KommuneTerritorialgrenseId::class)
class KommuneTerritorialgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    var kommune: Kommune,

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

data class KommuneTerritorialgrenseId (val kommune: String = "", val territorialgrense: String = "") : Serializable