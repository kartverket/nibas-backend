package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "grunnkrets_delomraadegrense")
@Audited
@Entity
@IdClass(GrunnkretsDelomraadegrenseId::class)
class GrunnkretsDelomraadegrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "grunnkrets_fk")
    var grunnkrets: Grunnkrets,

    @Id
    @ManyToOne
    @JoinColumn(name = "delomraadegrense_fk")
    var delomraadegrense: Delomraadegrense,

    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter

) : GrenseKobling {
    override fun getGrense(): Grense = delomraadegrense
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }
}

data class GrunnkretsDelomraadegrenseId (val grunnkrets: String = "", val delomraadegrense: String = "") : Serializable