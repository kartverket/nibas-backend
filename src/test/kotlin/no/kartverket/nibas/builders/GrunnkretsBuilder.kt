package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Grunnkrets
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate

class GrunnkretsBuilder(private val id: String = "1") {

    private var kommunenummer: String = "UUID-1"
    private var lokalId: String = LOKALID
    private var versjonId: String = VERSJON_33
    private var navnerom: String = NAVNEROM

    private var oppdateringsdato: LocalDate = LocalDate.now()
    private var datafangstdato: LocalDate = LocalDate.now()
    private var gyldigFra: LocalDate = LocalDate.now()
    private var gyldigTil: LocalDate = LocalDate.now()
    private var informasjonstekst = INFORMASJONTEKST
    private var opphav = OPPHAV

    fun build(): Grunnkrets {
        val grunnkrets = Grunnkrets(id = this.id)

        grunnkrets.datafangstdato = datafangstdato

        grunnkrets.lokalid = lokalId
        grunnkrets.versjonid = versjonId
        grunnkrets.navnerom = navnerom
        grunnkrets.kommunenummer = kommunenummer
        grunnkrets.auditInfo.oppdateringsdato = oppdateringsdato

        grunnkrets.auditInfo.oppdateringsdato = oppdateringsdato
        grunnkrets.gyldigFra = gyldigFra
        grunnkrets.gyldigTil = gyldigTil
        grunnkrets.setInfo(informasjonstekst)
        grunnkrets.opphav = opphav

        return grunnkrets
    }

    fun withOppdateringsdato(dateTime: LocalDate): GrunnkretsBuilder {
        this.oppdateringsdato = dateTime
        return this
    }

    fun withDatafangstdato(dateTime: LocalDate): GrunnkretsBuilder {
        this.datafangstdato = dateTime
        return this
    }

    fun withGyldigFra(dateTime: LocalDate): GrunnkretsBuilder {
        this.gyldigFra = dateTime
        return this
    }

    fun withGyldigTil(dateTime: LocalDate): GrunnkretsBuilder {
        this.gyldigTil = dateTime
        return this
    }

}
