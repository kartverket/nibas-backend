package no.kartverket.nibas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Her brukes ikke UUID fordi id-en er en del av datasettet, kolonne i tabellen.
 */

@Table(name = "landkode")
@Entity
class Landkode {
    @Id
    @Column(name = "identifier", nullable = false)
    lateinit var id: String

    @Column(name = "description")
    var description: String? = null
}
