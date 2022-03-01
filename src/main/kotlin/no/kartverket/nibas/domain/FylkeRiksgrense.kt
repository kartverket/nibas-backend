package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*
import java.io.Serializable

@Table(name = "fylke_riksgrense")
@Audited
@Entity
@IdClass(FylkeRiksgrenseId::class)
class FylkeRiksgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    var fylke: Fylke,

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
data class FylkeRiksgrenseId (val fylke: String = "", val riksgrense: String = "") : Serializable