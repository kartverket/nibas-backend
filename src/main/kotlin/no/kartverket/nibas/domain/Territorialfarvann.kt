package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "territorialfarvann")
@Entity
class Territorialfarvann {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}