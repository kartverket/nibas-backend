package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class StatistiskGrense : Grense() {

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "datafangstdato")
    override var datafangstdato: LocalDate? = null

    @Column(name = "oppdateringsdato")
    override var oppdateringsdato: LocalDate = LocalDate.now()

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "gyldigfra")
    override var gyldigFra: LocalDate? = null

    @Column(name = "gyldigtil")
    override var gyldigTil: LocalDate? = null

    @Column(name = "informasjon")
    var informasjon: String? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: LocalDate? = null

    @Embedded
    var endringsflagg: Endringsflagg? = Endringsflagg()

    @Column(name = "opphav")
    override var opphav: String? = null

    @Embedded
    var kopidata: Kopidata? = Kopidata()

    @Embedded
    var registreringsversjon: Registreringsversjon? = Registreringsversjon()

    @ManyToOne
    @JoinColumn(name = "foelgerterrengdetalj")
    var foelgerterrengdetalj: Terrengdetaljkode? = null

    @Column(name = "foelgerteiggrense")
    var foelgerteiggrense: Boolean? = null

    @ManyToOne
    @JoinColumn(name = "noeyaktighetsklasse")
    var noeyaktighetsklasse: Noeyaktighetsklassekode? = null
}
