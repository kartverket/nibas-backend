package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "kommune_kommunegrense")
@Audited
@Entity
@IdClass(KommuneKommunegrenseId::class)
class KommuneKommunegrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    var kommune: Kommune,

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

data class KommuneKommunegrenseId (val kommune: String = "", val kommunegrense: String = "") : Serializable