package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.geojson.metadata.Dokref
import no.kartverket.nibas.api.v1.common.geojson.metadata.TekstHolder
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "dokumentasjonsreferanse")
@Audited
@Entity
class Dokumentasjonsreferanse {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @Column(name = "objtype")
    var objtype: String? = null

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

    @ManyToOne
    @JoinColumn(name = "grense_fk")
    var grenseFk: Grense? = null

    @ManyToOne
    @JoinColumn(name = "lovvirkeomraade_fk")
    var lovvirkeomraadeFk: Lovvirkeomraade? = null

    @ManyToOne
    @JoinColumn(name = "detsaerskilteomraadet_fk")
    var detsaerskilteomraadetFk: Detsaerskilteomraadet? = null

    @ManyToOne
    @JoinColumn(name = "fiskerisone_fk")
    var fiskerisoneFk: Fiskerisone? = null

    @ManyToOne
    @JoinColumn(name = "fiskevernsone_fk")
    var fiskevernsoneFk: Fiskevernsone? = null

    @ManyToOne
    @JoinColumn(name = "graasonen_fk")
    var graasonenFk: Graasonen? = null

    @ManyToOne
    @JoinColumn(name = "indrefarvann_fk")
    var indrefarvannFk: Indrefarvann? = null

    @ManyToOne
    @JoinColumn(name = "kontinentalsokkel_fk")
    var kontinentalsokkelFk: Kontinentalsokkel? = null

    @ManyToOne
    @JoinColumn(name = "norgesoekonomiskesone_fk")
    var norgesoekonomiskesoneFk: Norgesoekonomiskesone? = null

    @ManyToOne
    @JoinColumn(name = "sjoeterritorium_fk")
    var sjoeterritoriumFk: Sjoeterritorium? = null

    @ManyToOne
    @JoinColumn(name = "territorialfarvann_fk")
    var territorialfarvannFk: Territorialfarvann? = null

    @ManyToOne
    @JoinColumn(name = "tilstoetendesone_fk")
    var tilstoetendesoneFk: Tilstoetendesone? = null

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
@Audited
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
