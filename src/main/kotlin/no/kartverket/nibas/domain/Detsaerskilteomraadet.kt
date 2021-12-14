package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "detsaerskilteomraadet")
@Entity
class Detsaerskilteomraadet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}