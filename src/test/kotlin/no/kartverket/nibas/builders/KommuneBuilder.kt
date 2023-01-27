package no.kartverket.nibas.builders

import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate
import java.time.ZonedDateTime

class KommuneBuilder(
    private val id: String = "1",
    private var kommunenummer: String = "UUID-1",
    private var lokalId: Lokalid = Lokalid("123"),
    private var navn: String = "Bergen",
    private var spraak: String = "NOR",
    private var samiskforvaltningsomraade: Boolean = false,
    private var endretdato: ZonedDateTime = ZonedDateTime.now(),
    private var grunnkretser: List<Grunnkrets> = listOf(),
    private var stemmekretser: List<Stemmekrets> = listOf(),
    private var gyldigFra: LocalDate = LocalDate.now(),
    private var gyldigTil: LocalDate? = null,
    private var utledetFra: Kommune? = null
) {

    companion object {
        val DEFAULT_KOMMUNE = KommuneBuilder().build()
    }

    fun withNavn(navn: String, spraak: String = "NOR"): KommuneBuilder {
        this.navn = navn
        this.spraak = spraak
        return this
    }

    fun build(): Kommune {
        val kommune = Kommune(id = this.id, lokalid = lokalId.value)
        val spraakkode = Spraakkode()
        spraakkode.id = spraak
        spraakkode.description = spraak

        val administrativenhetnavn = Administrativenhetnavn()
        administrativenhetnavn.navn = navn
        administrativenhetnavn.spraak = spraakkode

        kommune.administrativenhetnavn = mutableSetOf(administrativenhetnavn)
        administrativenhetnavn.kommuneFk = kommune

        kommune.kommunenummer = kommunenummer
        kommune.samiskforvaltningsomraade = samiskforvaltningsomraade
        kommune.auditInfo.endretdato = endretdato
        kommune.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        kommune.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))

        grunnkretser.forEach { kommune.addGrunnkrets(it) }
        stemmekretser.forEach { kommune.addStemmekrets(it) }

        kommune.gyldighet = Gyldighet(gyldigFra, gyldigTil)
        kommune.utledetFra = utledetFra

        return kommune
    }

    fun buildWithoutAdministrativenhetnavn(): Kommune {
        return build().apply { this.administrativenhetnavn.clear() }
    }

    fun withLokalId(lokalId: String): KommuneBuilder {
        this.lokalId = Lokalid(lokalId)
        return this
    }

    fun withGrunnkrets(grunnkrets: Grunnkrets): KommuneBuilder {
        this.grunnkretser = this.grunnkretser.plus(grunnkrets)
        return this
    }

    fun withStemmekrets(stemmekrets: Stemmekrets): KommuneBuilder {
        this.stemmekretser = this.stemmekretser.plus(stemmekrets)
        return this
    }

}
