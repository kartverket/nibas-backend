package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Table(name = "dokumentasjonsreferanse_dokumentlink")
@Audited
@Entity
class DokumentasjonsreferanseDokumentlink {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @OneToOne
    @JoinColumn(name = "dokumentasjonsreferanse_fk")
    var dokumentasjonsreferanseFk: Dokumentasjonsreferanse? = null

    @Column(name = "dokumentlink")
    var dokumentlink: String? = null
}
