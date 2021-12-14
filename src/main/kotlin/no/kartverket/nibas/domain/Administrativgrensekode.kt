package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "administrativgrensekode")
@Entity
class Administrativgrensekode {
    @Id
    
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}