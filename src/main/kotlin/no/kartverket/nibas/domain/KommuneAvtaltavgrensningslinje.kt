package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "kommune_avtaltavgrensningslinje")
@Audited
@Entity
@IdClass(KommuneAvtaltavgrensningslinjeId::class)
class KommuneAvtaltavgrensningslinje (

    @Id
    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    var kommune: Kommune,

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

data class KommuneAvtaltavgrensningslinjeId (val kommune: String = "", val avtaltavgrensningslinje: String = "") : Serializable