package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "fiskerisone")
@Entity
class Fiskerisone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}