package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*

@Table(name = "nasjon_riksgrense")
@Audited
@Entity
@IdClass(NasjonRiksgrenseId::class)
class NasjonRiksgrense (

    @Id
    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjon: Nasjon,

    @Id
    @ManyToOne
    @JoinColumn(name = "riksgrense_fk")
    var riksgrense: Riksgrense,

    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter

) : GrenseKobling {
    override fun getGrense(): Grense = riksgrense
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }
}

data class NasjonRiksgrenseId (val nasjon: String = "", val riksgrense: String = "") : Serializable