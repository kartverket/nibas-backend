package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "stemmekrets_avtaltavgrensningslinje")
@Audited
@Entity
@IdClass(StemmekretsAvtaltavgrensningslinjeId::class)
class StemmekretsAvtaltavgrensningslinje (

    @Id
    @ManyToOne
    @JoinColumn(name = "stemmekrets_fk")
    var stemmekrets: Stemmekrets,

    @Id
    @ManyToOne
    @JoinColumn(name = "avtaltavgrensningslinje_fk")
    var avtaltavgrensningslinje: Avtaltavgrensningslinje,

    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter

) : GrenseKobling {
    override fun getGrense(): Grense = avtaltavgrensningslinje
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }
}
data class StemmekretsAvtaltavgrensningslinjeId (val stemmekrets: String = "", val avtaltavgrensningslinje: String = "") : Serializable
