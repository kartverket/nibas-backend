package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "fastsettingstype")
@Audited
@Entity
class Fastsettingstype {

    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}