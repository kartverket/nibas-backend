package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "grunnkrets_grunnkretsgrense")
@Audited
@Entity
@IdClass(GrunnkretsGrunnkretsgrenseId::class)
class GrunnkretsGrunnkretsgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "grunnkrets_fk")
    var grunnkrets: Grunnkrets,

    @Id
    @ManyToOne
    @JoinColumn(name = "grunnkretsgrense_fk")
    var grunnkretsgrense: Grunnkretsgrense,

    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter

) : GrenseKobling {
    override fun getGrense(): Grense = grunnkretsgrense
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }
}

data class GrunnkretsGrunnkretsgrenseId (val grunnkrets: String = "", val grunnkretsgrense: String = "") : Serializable