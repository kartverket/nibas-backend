package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "lovvirkeomraade")
@Entity
class Lovvirkeomraade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}