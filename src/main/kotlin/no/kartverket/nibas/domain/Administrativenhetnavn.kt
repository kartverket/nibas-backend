package no.kartverket.nibas.domain

import no.kartverket.nibas.sporing.Laasbar
import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Version

@Table(name = "administrativenhetnavn")
@Audited
@Entity
class Administrativenhetnavn : Laasbar {

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @Column(name = "objtype")
    var objtype: String? = null

    @Version
    override var version: Int = 1

    @Column(name = "navn", nullable = false)
    var navn: String? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "spraak", nullable = false)
    var spraak: Spraakkode? = null

    @Column(name = "rekkefoelge")
    var rekkefoelge: Int? = null

    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    var fylkeFk: Fylke? = null

    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    var kommuneFk: Kommune? = null

    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjonFk: Nasjon? = null
}
