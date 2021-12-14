package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "fastsettingstype")
@Entity
class Fastsettingstype {

    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}