package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "sjoeterritorium")
@Entity
class Sjoeterritorium {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}