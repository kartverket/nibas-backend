package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "typeendring")
@Entity
class Typeendring {
    @Id
    
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}