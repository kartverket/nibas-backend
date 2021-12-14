package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "spraakkode")
@Entity
class Spraakkode {

    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null

    @Column(name = "description")
    var description: String? = null
}