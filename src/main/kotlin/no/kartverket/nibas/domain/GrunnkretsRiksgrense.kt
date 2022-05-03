package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "grunnkrets_riksgrense")
@Audited
@Entity
@IdClass(GrunnkretsRiksgrenseId::class)
class GrunnkretsRiksgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "grunnkrets_fk")
    var grunnkrets: Grunnkrets,

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

data class GrunnkretsRiksgrenseId (val grunnkrets: String = "", val riksgrense: String = "") : Serializable