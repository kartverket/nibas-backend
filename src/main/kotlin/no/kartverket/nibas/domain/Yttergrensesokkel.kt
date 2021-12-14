package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "yttergrensesokkel")
@Entity
class Yttergrensesokkel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null
}