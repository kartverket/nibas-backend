package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "landkode")
@Audited
@Entity
class Landkode {
    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null

    @Column(name = "description")
    var description: String? = null
}