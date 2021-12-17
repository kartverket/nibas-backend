package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "terrengdetaljkode")
@Audited
@Entity
class Terrengdetaljkode {

    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}