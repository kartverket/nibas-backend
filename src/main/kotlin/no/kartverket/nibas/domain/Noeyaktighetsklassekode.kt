package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "noeyaktighetsklassekode")
@Audited
@Entity
class Noeyaktighetsklassekode {
    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}