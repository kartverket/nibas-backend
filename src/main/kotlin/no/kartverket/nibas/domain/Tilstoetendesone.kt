package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "tilstoetendesone")
@Entity
class Tilstoetendesone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}