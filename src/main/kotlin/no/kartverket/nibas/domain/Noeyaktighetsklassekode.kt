package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "noeyaktighetsklassekode")
@Entity
class Noeyaktighetsklassekode {
    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}