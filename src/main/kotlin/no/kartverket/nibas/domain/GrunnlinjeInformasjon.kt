package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "grunnlinje_informasjon")
@Audited
@Entity
class GrunnlinjeInformasjon {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "grunnlinje_fk")
    var grunnlinjeFk: Grunnlinje? = null

    @Column(name = "informasjon")
    var informasjon: String? = null
}
