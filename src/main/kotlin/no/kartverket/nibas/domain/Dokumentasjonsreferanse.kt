package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.geojson.metadata.DokumentasjonsreferanseDTO
import no.kartverket.nibas.api.v1.common.geojson.metadata.TekstHolder
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldig
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import no.kartverket.nibas.sporing.Sporbar
import java.time.LocalDate
import java.util.*
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version

@Table(name = "dokumentasjonsreferanse")
@Entity
class Dokumentasjonsreferanse(
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString(),

    @Column(name = "lokalid", nullable = false, updatable = false)
    val lokalid: String = UUID.randomUUID().toString()
) : Laasbar, Sporbar, Gyldig {

    @Column(name = "foerstedigitaliseringsdato", nullable = false, updatable = false)
    var foerstedigitaliseringsdato: LocalDate = LocalDate.now()

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @Embedded
    override var gyldighet: Gyldighet = Gyldighet()

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

    /*
     *  Nullable av historiske årsaker (gamle dokumentasjonsreferanser).
     *  Nye dokumentasjonsreferanser skal håndtere denne som om at den var not null .
    */
    @Column(name = "vedtak_gyldig_fra")
    var vedtakGyldigFra: LocalDate? = null

    @Column(name = "vedtak_gyldig_til")
    var vedtakGyldigTil: LocalDate? = null

    @OneToMany(mappedBy = "dokumentasjonsreferanseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var dokumentlenker: MutableSet<DokumentasjonsreferanseDokumentlink> = mutableSetOf()

    @OneToMany(mappedBy = "dokumentasjonsreferanseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var internReferanserKartverket: MutableSet<DokumentasjonsreferanseInternReferanseKartverket> = mutableSetOf()

    fun hasChanges(dokumentreferanseDto: DokumentasjonsreferanseDTO) =
        if (dokumentreferanseDto.shouldArchive) true
        else if (this.id != dokumentreferanseDto.id) true
        else if (this.fastsettingsdato != dokumentreferanseDto.fastsettingsdato) true
        else if (this.vedtakGyldigTil != dokumentreferanseDto.vedtakGyldigTil) true
        else if (this.vedtakGyldigFra != dokumentreferanseDto.vedtakGyldigFra) true
        else if (this.rettskildetittel != dokumentreferanseDto.rettskildeTittel) true
        else if (rettskildeid != dokumentreferanseDto.rettskildeId) true
        else if (hjemmel != dokumentreferanseDto.hjemmel) true
        else if (fastsettingsmyndighet != dokumentreferanseDto.fastsettingsmyndighet) true
        else if (vedtakGyldigFra != dokumentreferanseDto.vedtakGyldigFra) true
        else if (vedtakGyldigTil != dokumentreferanseDto.vedtakGyldigTil) true
        else if (dokumentlenker.filter {
                !dokumentreferanseDto.dokumentlenker.map {
                    it.beskrivelse
                }.contains(it.dokumentlink)
            }.isNotEmpty()) true
        else if (internReferanserKartverket.filter {
                !dokumentreferanseDto.internReferanserKartverket.map {
                    it.beskrivelse
                }.contains(it.internReferanseKartverket)
            }.isNotEmpty()) true
        else false

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

    fun toDokRef(): DokumentasjonsreferanseDTO = DokumentasjonsreferanseDTO(
        id = this.id,
        dokumentlenker = this.dokumentlenker
            .filter { it.dokumentlink != null }
            .mapNotNull { it.dokumentlink?.let { link -> TekstHolder(it.id, link) } }
            .sortedBy { it.beskrivelse },
        fastsettingsdato = this.fastsettingsdato ?: throw RuntimeException("Fastsettingsdato må angis"),
        fastsettingsmyndighet = this.fastsettingsmyndighet,
        hjemmel = this.hjemmel,
        internReferanserKartverket = this.internReferanserKartverket
            .filter { it.internReferanseKartverket != null }
            .map { TekstHolder(it.id, it.internReferanseKartverket ?: throw RuntimeException("InternreferanseKartverket må angis")) }
            .sortedBy { it.beskrivelse },
        rettskildeId = this.rettskildeid,
        rettskildeTittel = this.rettskildetittel ?: throw RuntimeException("Rettskildetittel må angis"),
        vedtakGyldigTil = this.vedtakGyldigTil,
        vedtakGyldigFra = this.vedtakGyldigFra
    )

    override fun hashCode(): Int {
        var result = lokalid.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + foerstedigitaliseringsdato.hashCode()
        result = 31 * result + auditInfo.hashCode()
        result = 31 * result + gyldighet.hashCode()
        result = 31 * result + (objtype?.hashCode() ?: 0)
        result = 31 * result + version
        result = 31 * result + (rettskildetittel?.hashCode() ?: 0)
        result = 31 * result + (rettskildeid?.hashCode() ?: 0)
        result = 31 * result + (hjemmel?.hashCode() ?: 0)
        result = 31 * result + (fastsettingsmyndighet?.hashCode() ?: 0)
        result = 31 * result + (fastsettingsdato?.hashCode() ?: 0)
        result = 31 * result + vedtakGyldigFra.hashCode()
        result = 31 * result + (vedtakGyldigTil?.hashCode() ?: 0)
        result = 31 * result + dokumentlenker.hashCode()
        result = 31 * result + internReferanserKartverket.hashCode()
        return result
    }

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
