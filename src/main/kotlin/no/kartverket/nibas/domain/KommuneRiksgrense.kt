package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "kommune_riksgrense")
@Audited
@Entity
@IdClass(KommuneRiksgrenseId::class)
class KommuneRiksgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    var kommune: Kommune,

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

data class KommuneRiksgrenseId (val kommune: String = "", val riksgrense: String = "") : Serializable