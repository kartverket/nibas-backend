package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "fiskevernsone")
@Entity
class Fiskevernsone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}