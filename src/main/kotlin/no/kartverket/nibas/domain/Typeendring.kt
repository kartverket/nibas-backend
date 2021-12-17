package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "typeendring")
@Audited
@Entity
class Typeendring {
    @Id
    
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}