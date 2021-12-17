package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "administrativgrensekode")
@Audited
@Entity
class Administrativgrensekode {
    @Id
    
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}