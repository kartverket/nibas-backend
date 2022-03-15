package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "kommunegrense_informasjon")
@Audited
@Entity
class KommunegrenseInformasjon : GrenseInfo {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "kommunegrense_fk")
    var kommunegrenseFk: Kommunegrense? = null

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
