package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "norgesoekonomiskesone")
@Entity
class Norgesoekonomiskesone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}