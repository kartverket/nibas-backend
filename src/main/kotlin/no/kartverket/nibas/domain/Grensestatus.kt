package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "grensestatus")
@Audited
@Entity
class Grensestatus {
    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}