package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.geojson.metadata.Dokref
import no.kartverket.nibas.api.v1.common.geojson.metadata.TekstHolder
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import java.time.LocalDate
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Version

@Table(name = "dokumentasjonsreferanse")
@Entity
class Dokumentasjonsreferanse : Laasbar {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @Column(name = "lokalid", nullable = false, updatable = false)
    val lokalid: String = UUID.randomUUID().toString()

    @Column(name = "foerstedigitaliseringsdato", nullable = false, updatable = false)
    var foerstedigitaliseringsdato: LocalDate = LocalDate.now()

    @Embedded
    var auditInfo: AuditInfo = AuditInfo()

    @Embedded
    var gyldighet: Gyldighet = Gyldighet()

    @Column(name = "objtype")
    var objtype: String? = null

    @Version
    override var version: Int = 1

    @Column(name = "rettskildetittel", nullable = false)
    var rettskildetittel: String? = null

    @Column(name = "rettskildeid")
    var rettskildeid: String? = null

    @Column(name = "hjemmel")
    var hjemmel: String? = null

    @Column(name = "fastsettingsmyndighet")
    var fastsettingsmyndighet: String? = null

    @Column(name = "fastsettingsdato", nullable = false)
    var fastsettingsdato: LocalDate? = null

    @OneToMany(mappedBy = "dokumentasjonsreferanseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var dokumentlenker: MutableSet<DokumentasjonsreferanseDokumentlink> = mutableSetOf()

    @OneToMany(mappedBy = "dokumentasjonsreferanseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var internReferanserKartverket: MutableSet<DokumentasjonsreferanseInternReferanseKartverket> = mutableSetOf()

    fun addDokumentLenke(): DokumentasjonsreferanseDokumentlink {
        val dokumentLink = DokumentasjonsreferanseDokumentlink()
        dokumentlenker.add(dokumentLink)
        dokumentLink.dokumentasjonsreferanseFk = this
        return dokumentLink
    }

    fun addInternReferanse(): DokumentasjonsreferanseInternReferanseKartverket {
        val internReferanse = DokumentasjonsreferanseInternReferanseKartverket()
        internReferanserKartverket.add(internReferanse)
        internReferanse.dokumentasjonsreferanseFk = this
        return internReferanse
    }

    fun toDokRef(): Dokref = Dokref(
        id = this.id,
        dokumentlenker = this.dokumentlenker
            .filter { it.dokumentlink != null }
            .map { TekstHolder(it.id, it.dokumentlink!!) }
            .sortedBy { it.beskrivelse },
        fastsettingsdato = this.fastsettingsdato!!,
        fastsettingsmyndighet = this.fastsettingsmyndighet,
        hjemmel = this.hjemmel,
        internReferanserKartverket = this.internReferanserKartverket
            .filter { it.internReferanseKartverket != null }
            .map { TekstHolder(it.id, it.internReferanseKartverket!!) }
            .sortedBy { it.beskrivelse },
        rettskildeId = this.rettskildeid,
        rettskildeTittel = this.rettskildetittel!!
    )
}

@Table(name = "dokumentasjonsreferanse_dokumentlink")
@Entity
class DokumentasjonsreferanseDokumentlink {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "dokumentasjonsreferanse_fk")
    var dokumentasjonsreferanseFk: Dokumentasjonsreferanse? = null

    @Column(name = "dokumentlink")
    var dokumentlink: String? = null
}

@Table(name = "dokumentasjonsreferanse_internreferansekartverket")
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
