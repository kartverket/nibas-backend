package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate
import java.time.ZonedDateTime

class KommuneBuilder(
    private val id: String = "a7dabf8d-4597-4735-b3cd-db462c61c39e",
    private var kommunenummer: String = "UUID-1",
    private var unikLokalid: UnikLokalid = UnikLokalid("fe328f8d-fbde-4c21-ab28-cbc583be401f", Flatetype.KOMMUNE),
    private var navn: String? = null,
    private var spraak: String = "NOR",
    private var samiskforvaltningsomraade: Boolean = false,
    private var endretdato: ZonedDateTime = ZonedDateTime.now(),
    private var grunnkretser: List<Grunnkrets> = listOf(),
    private var stemmekretser: List<Stemmekrets> = listOf(),
    private var gyldigFra: LocalDate = LocalDate.now(),
    private var gyldigTil: LocalDate? = null,
    private var utledetFra: Kommune? = null,
    private var administrativenhetnavn: List<Administrativenhetnavn> = emptyList(),
) {

    companion object {
        val DEFAULT_KOMMUNE = KommuneBuilder().withNavn("Bergen").build()
    }

    fun withNavn(navn: String, spraak: String = "NOR"): KommuneBuilder {
        this.navn = navn
        this.spraak = spraak
        return this
    }

    fun build(): Kommune {
        val kommune = buildUtenUnikLokalidOgTilkntninger()
        kommune.addKommuneTilUnikLokalid(unikLokalid)

        grunnkretser.forEach { kommune.addGrunnkrets(it) }
        stemmekretser.forEach { kommune.addStemmekrets(it) }
        administrativenhetnavn.forEach { kommune.addAdministrativenhetNavn(it) }

        return kommune
    }

    fun buildUtenUnikLokalidOgTilkntninger(): Kommune {
        val kommune = Kommune(id = this.id, lokalid = unikLokalid.lokalid)

        if (navn != null) {
            val spraakkode = Spraakkode()
            spraakkode.id = spraak
            spraakkode.description = spraak

            val admNavn = Administrativenhetnavn()
            admNavn.navn = navn
            admNavn.spraak = spraakkode

            administrativenhetnavn = listOf(admNavn)
        }

        kommune.kommunenummer = kommunenummer
        kommune.samiskforvaltningsomraade = samiskforvaltningsomraade
        kommune.auditInfo.endretdato = endretdato
        kommune.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        kommune.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))

        kommune.gyldighet = Gyldighet(gyldigFra, gyldigTil)
        kommune.utledetFra = utledetFra

        return kommune
    }

    fun withGrunnkrets(grunnkrets: Grunnkrets): KommuneBuilder {
        this.grunnkretser = this.grunnkretser.plus(grunnkrets)
        return this
    }

    fun withStemmekrets(stemmekrets: Stemmekrets): KommuneBuilder {
        this.stemmekretser = this.stemmekretser.plus(stemmekrets)
        return this
    }

    fun withGyldigFra(dato: LocalDate): KommuneBuilder {
        this.gyldigFra = dato
        return this
    }

}
