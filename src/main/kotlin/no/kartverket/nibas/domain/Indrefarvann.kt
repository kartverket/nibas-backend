package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "indrefarvann")
@Entity
class Indrefarvann {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}