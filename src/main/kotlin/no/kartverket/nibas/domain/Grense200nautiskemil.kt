package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "grense200nautiskemil")
@Entity
class Grense200nautiskemil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}