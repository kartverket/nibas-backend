package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table


@Table(name = "avtaltavgrensningslinje_informasjon")
@Audited
@Entity
class AvtaltavgrensningslinjeInformasjon {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "avtaltavgrensningslinje_fk")
    var avtaltavgrensningslinjeFk: Avtaltavgrensningslinje? = null

    
    @Column(name = "informasjon")
    var informasjon: String? = null
}
