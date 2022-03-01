package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*
import java.io.Serializable

@Table(name = "fylke_fylkesgrense")
@Audited
@Entity
@IdClass(FylkeFylkesgrenseId::class)
class FylkeFylkesgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    var fylke: Fylke,

    @Id
    @ManyToOne
    @JoinColumn(name = "fylkesgrense_fk")
    var fylkesgrense: Fylkesgrense,
    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter

) : GrenseKobling {

    override fun getGrense(): Grense = fylkesgrense
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }
}

data class FylkeFylkesgrenseId (val fylke: String = "", val fylkesgrense: String = "") : Serializable