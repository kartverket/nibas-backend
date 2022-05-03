package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class AdministrativGrense : GrenseMedDokumentasjonsreferanser() {

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: LocalDate? = null

    @Column(name = "oppdateringsdato")
    override var oppdateringsdato: LocalDate = LocalDate.now()

    @Embedded
    var endringsflagg: Endringsflagg? = Endringsflagg()

    @Column(name = "opphav")
    override var opphav: String? = null

    @Embedded
    var registreringsversjon: Registreringsversjon? = Registreringsversjon()

    @Column(name = "gyldigfra")
    override var gyldigFra: LocalDate? = null

    @Column(name = "gyldigtil")
    override var gyldigTil: LocalDate? = null

    @Embedded
    var kopidata: Kopidata? = Kopidata()

    @Column(name = "datafangstdato")
    override var datafangstdato: LocalDate? = null

    @Column(name = "omtvistet")
    var omtvistet: Boolean? = null

    @ManyToOne
    @JoinColumn(name = "foelgerterrengdetalj")
    var foelgerterrengdetalj: Terrengdetaljkode? = null

    @ManyToOne
    @JoinColumn(name = "noeyaktighetsklasse")
    var noeyaktighetsklasse: Noeyaktighetsklassekode? = null
}
