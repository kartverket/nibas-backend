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
class NasjonInformasjon(nasjon: Nasjon) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjonFk: Nasjon? = nasjon

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
