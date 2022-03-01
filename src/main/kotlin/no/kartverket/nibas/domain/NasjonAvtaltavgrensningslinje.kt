package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "nasjon_avtaltavgrensningslinje")
@Audited
@Entity
@IdClass(NasjonAvtaltavgrensningslinjeId::class)
class NasjonAvtaltavgrensningslinje (

    @Id
    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjon: Nasjon,

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

data class NasjonAvtaltavgrensningslinjeId (val nasjon: String = "", val avtaltavgrensningslinje: String = "") : Serializable