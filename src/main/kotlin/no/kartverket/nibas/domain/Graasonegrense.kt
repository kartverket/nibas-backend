package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "graasonegrense")
@Audited
@Entity
class Graasonegrense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}