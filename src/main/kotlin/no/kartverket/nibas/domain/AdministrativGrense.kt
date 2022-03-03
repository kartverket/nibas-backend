package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.geojson.Dokref
import org.hibernate.envers.Audited
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class AdministrativGrense : Grense() {

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "sluttdato")
    var sluttdato: ZonedDateTime? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: ZonedDateTime? = null

    @Column(name = "oppdateringsdato")
    var oppdateringsdato: ZonedDateTime = ZonedDateTime.now()

    @Embedded
    var endringsflagg: Endringsflagg? = Endringsflagg()

    @Embedded
    var kvalitet: PosisjonskvalitetEmbeddable? = PosisjonskvalitetEmbeddable()

    @Column(name = "opphav")
    var opphav: String? = null

    @Embedded
    var registreringsversjon: Registreringsversjon? = Registreringsversjon()

    @Column(name = "gyldigfra")
    var gyldigFra: ZonedDateTime? = null

    @Column(name = "gyldigtil")
    var gyldigTil: ZonedDateTime? = null

    @Embedded
    var kopidata: Kopidata? = Kopidata()

    @Column(name = "datafangstdato")
    var datafangstdato: ZonedDateTime? = null

    @Column(name = "omtvistet")
    var omtvistet: Boolean? = null

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    var grensestatus: Grensestatus? = null

    @ManyToOne
    @JoinColumn(name = "fastsettingstype")
    var fastsettingstype: Fastsettingstype? = null

    @ManyToOne
    @JoinColumn(name = "foelgerterrengdetalj")
    var foelgerterrengdetalj: Terrengdetaljkode? = null

    @ManyToOne
    @JoinColumn(name = "noeyaktighetsklasse")
    var noeyaktighetsklasse: Noeyaktighetsklassekode? = null

    abstract fun getInformasjonselementer(): List<String>
    abstract fun getDokumentasjonsreferanser(): List<Dokumentasjonsreferanse>

    fun getListeAvDokumentasjonsreferanser(): List<Dokref> {
        return getDokumentasjonsreferanser().map { it.toDokRef() }
    }
}
