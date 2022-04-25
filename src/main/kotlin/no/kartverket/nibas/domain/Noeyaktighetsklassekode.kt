package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Table(name = "noeyaktighetsklassekode")
@Audited
@Entity
class Noeyaktighetsklassekode {
    @Id
    @Column(name = "identifier", nullable = false)
    lateinit var id: String

    @Column(name = "description")
    var description: String? = null
}
