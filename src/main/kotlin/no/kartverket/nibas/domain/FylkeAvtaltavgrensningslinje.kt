package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*
import java.io.Serializable

@Table(name = "fylke_avtaltavgrensningslinje")
@Audited
@Entity
@IdClass(FylkeAvtaltavgrensningslinjeId::class)
class FylkeAvtaltavgrensningslinje (

    @Id
    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    var fylke: Fylke,

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

data class FylkeAvtaltavgrensningslinjeId (val fylke: String = "", val avtaltavgrensningslinje: String = "") : Serializable