package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "fylkesgrense_informasjon")
@Audited
@Entity
class FylkesgrenseInformasjon {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "fylkesgrense_fk")
    var fylkesgrenseFk: Fylkesgrense? = null

    @Column(name = "informasjon")
    var informasjon: String? = null
}
