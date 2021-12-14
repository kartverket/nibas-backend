package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "maalemetode")
@Entity
class Maalemetode {
    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}