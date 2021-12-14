package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "kontinentalsokkel")
@Entity
class Kontinentalsokkel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}