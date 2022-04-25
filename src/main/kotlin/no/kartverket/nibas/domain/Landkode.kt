package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * Her brukes ikke UUID fordi id-en er en del av datasettet, kolonne i tabellen.
 */
@Table(name = "landkode")
@Audited
@Entity
class Landkode {
    @Id
    @Column(name = "identifier", nullable = false)
    lateinit var id: String

    @Column(name = "description")
    var description: String? = null
}
