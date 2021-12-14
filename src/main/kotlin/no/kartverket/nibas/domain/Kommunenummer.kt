package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "kommunenummer")
@Entity
class Kommunenummer {

    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null

    @Column(name = "description")
    var description: String? = null
}