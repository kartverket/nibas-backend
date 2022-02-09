package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "administrativenhetnavn")
@Audited
@Entity
class Administrativenhetnavn {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()
    
    @Column(name = "objtype")
    var objtype: String? = null

    
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
