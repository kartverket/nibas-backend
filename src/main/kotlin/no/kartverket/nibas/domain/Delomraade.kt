package no.kartverket.nibas.domain

import java.util.*
import jakarta.persistence.*

@Table(name = "delomraade")
@Entity
class Delomraade {

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()
}
