package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "lovvirkeomraade")
@Audited
@Entity
class Lovvirkeomraade {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()
}

@Table(name = "lovvirkeomraade_informasjon")
@Audited
@Entity
class LovvirkeomraadeInformasjon {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "lovvirkeomraade_fk")
    var lovvirkeomraadeFk: Lovvirkeomraade? = null

    @Column(name = "informasjon")
    var informasjon: String? = null
}
