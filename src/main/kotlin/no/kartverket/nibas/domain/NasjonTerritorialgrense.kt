package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "nasjon_territorialgrense")
@Audited
@Entity
@IdClass(NasjonTerritorialgrenseId::class)
class NasjonTerritorialgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjon: Nasjon,

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

data class NasjonTerritorialgrenseId (val nasjon: String = "", val territorialgrense: String = "") : Serializable