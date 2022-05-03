package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "grunnkrets_avtaltavgrensningslinje")
@Audited
@Entity
@IdClass(GrunnkretsAvtaltavgrensningslinjeId::class)
class GrunnkretsAvtaltavgrensningslinje (

    @Id
    @ManyToOne
    @JoinColumn(name = "grunnkrets_fk")
    var grunnkrets: Grunnkrets,

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

data class GrunnkretsAvtaltavgrensningslinjeId (val grunnkrets: String = "", val avtaltavgrensningslinje: String = "") : Serializable