package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "grunnkrets_fylkesgrense")
@Audited
@Entity
@IdClass(GrunnkretsFylkesgrenseId::class)
class GrunnkretsFylkesgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "grunnkrets_fk")
    var grunnkrets: Grunnkrets,

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

data class GrunnkretsFylkesgrenseId (val grunnkrets: String = "", val fylkesgrense: String = "") : Serializable