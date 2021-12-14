package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "nivaanummer")
@Entity
class Nivaanummer {
    @Id
    
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}