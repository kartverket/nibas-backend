package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import org.locationtech.jts.geom.LineString
import java.util.*
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class Grense : CommonMetadataProperties {

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @Column(name = "objtype")
    var objtype: String? = null

    @Column(name = "lokalid", nullable = false, updatable = false)
    override var lokalid: String = UUID.randomUUID().toString()

    @Column(name = "navnerom", nullable = false, updatable = false)
    override var navnerom: String = DEFAULT_NAVNEROM

    @Column(name = "grense", nullable = false)
    var grense: LineString? = null

    @Embedded
    var kvalitet: PosisjonskvalitetEmbeddable? = null

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    var grensestatus: Grensestatus? = null

    @ManyToOne
    @JoinColumn(name = "fastsettingstype")
    var fastsettingstype: Fastsettingstype? = null

    abstract var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse>

}
