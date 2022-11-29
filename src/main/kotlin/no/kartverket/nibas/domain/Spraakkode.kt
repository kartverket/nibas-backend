package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "spraakkode")
@Audited
@Entity
class Spraakkode {

    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null

    @Column(name = "description")
    var description: String? = null
}
