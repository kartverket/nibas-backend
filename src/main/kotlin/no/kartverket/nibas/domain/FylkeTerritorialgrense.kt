package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*
import java.io.Serializable

@Table(name = "fylke_territorialgrense")
@Audited
@Entity
@IdClass(FylkeTerritorialgrenseId::class)
class FylkeTerritorialgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    var fylke: Fylke,

    @Id
    @ManyToOne
    @JoinColumn(name = "territorialgrense_fk")
    var territorialgrense: Territorialgrense,

    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter

) : GrenseKobling {
    override fun getGrense(): Grense = territorialgrense
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }
}

data class FylkeTerritorialgrenseId (val fylke: String = "", val territorialgrense: String = "") : Serializable