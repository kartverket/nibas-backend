package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "nasjon_informasjon")
@Audited
@Entity
class NasjonInformasjon {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjonFk: Nasjon? = null

    @Column(name = "informasjon")
    var informasjon: String? = null
}
