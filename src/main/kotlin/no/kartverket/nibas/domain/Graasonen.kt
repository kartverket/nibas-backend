package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "graasonen")
@Entity
class Graasonen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}