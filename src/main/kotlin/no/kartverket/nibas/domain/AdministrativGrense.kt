package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class AdministrativGrense : Grense(), CommonMetadataProperties {

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "sluttdato")
    var sluttdato: ZonedDateTime? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: ZonedDateTime? = null

    @Column(name = "oppdateringsdato")
    override var oppdateringsdato: ZonedDateTime = ZonedDateTime.now()

    @Embedded
    var endringsflagg: Endringsflagg? = Endringsflagg()

    @Embedded
    override var kvalitet: PosisjonskvalitetEmbeddable? = null

    @Column(name = "opphav")
    override var opphav: String? = null

    @Embedded
    var registreringsversjon: Registreringsversjon? = Registreringsversjon()

    @Column(name = "gyldigfra")
    override var gyldigFra: ZonedDateTime? = null

    @Column(name = "gyldigtil")
    override var gyldigTil: ZonedDateTime? = null

    @Embedded
    var kopidata: Kopidata? = Kopidata()

    @Column(name = "datafangstdato")
    override var datafangstdato: ZonedDateTime? = null

    @Column(name = "omtvistet")
    var omtvistet: Boolean? = null

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    override var grensestatus: Grensestatus? = null

    @ManyToOne
    @JoinColumn(name = "fastsettingstype")
    override var fastsettingstype: Fastsettingstype? = null

    @ManyToOne
    @JoinColumn(name = "foelgerterrengdetalj")
    var foelgerterrengdetalj: Terrengdetaljkode? = null

    @ManyToOne
    @JoinColumn(name = "noeyaktighetsklasse")
    var noeyaktighetsklasse: Noeyaktighetsklassekode? = null

}
