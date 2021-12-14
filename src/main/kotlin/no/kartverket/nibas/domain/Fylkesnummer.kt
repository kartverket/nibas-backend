package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "fylkesnummer")
@Entity
class Fylkesnummer {
    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null

    @Column(name = "description")
    var description: String? = null
}