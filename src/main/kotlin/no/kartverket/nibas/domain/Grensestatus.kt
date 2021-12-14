package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "grensestatus")
@Entity
class Grensestatus {
    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}