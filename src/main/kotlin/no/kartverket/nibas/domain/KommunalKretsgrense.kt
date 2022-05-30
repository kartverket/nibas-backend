package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class KommunalKretsgrense  : Grense() {

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "datafangstdato")
    override var datafangstdato: LocalDate? = null

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

    @ManyToOne
    @JoinColumn(name = "noeyaktighetsklasse")
    var noeyaktighetsklasse: Noeyaktighetsklassekode? = null

    @Embedded
    var foelgerGrunnkretsgrenseIdentifikasjon: FoelgerGrunnkretsgrenseIdentifikasjon? = FoelgerGrunnkretsgrenseIdentifikasjon()
}

@Embeddable
data class FoelgerGrunnkretsgrenseIdentifikasjon(
    @Column(name = "foelgergrunnkretsgrense_lokalid")
    override var lokalid: String = "",

    @Column(name = "foelgergrunnkretsgrense_navnerom")
    override var navnerom: String = "",

    @Column(name = "foelgergrunnkretsgrense_versjonid")
    override var versjonid: String? = null
) : ContainsSosiIdentifikasjon
