package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "terrengdetaljkode")
@Entity
class Terrengdetaljkode {

    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}