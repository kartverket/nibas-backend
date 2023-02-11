package no.kartverket.nibas.domain

import java.util.*
import javax.persistence.*

@Table(name = "delomraade")
@Entity
class Delomraade {

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()
}
