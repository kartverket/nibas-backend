package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "dokumentasjonsreferanse_internreferansekartverket")
@Audited
@Entity
class DokumentasjonsreferanseInternReferanseKartverket {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "dokumentasjonsreferanse_fk")
    var dokumentasjonsreferanseFk: Dokumentasjonsreferanse? = null

    @Column(name = "internreferansekartverket")
    var internReferanseKartverket: String? = null
}
