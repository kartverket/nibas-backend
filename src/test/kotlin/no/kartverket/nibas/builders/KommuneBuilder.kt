package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Stemmekrets
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate

class KommuneBuilder(
    private val id: String = "1",
    private var kommunenummer: String = "UUID-1",
    private var lokalId: String = "123",
    private var navn: String = "Vestland",
    private var spraak: String = "NOR",
    private var samiskforvaltningsomraade: Boolean = false,
    private var oppdateringsdato: LocalDate = LocalDate.now(),
    private var grunnkretser: MutableList<Grunnkrets> = mutableListOf(),
    private var stemmekretser: MutableList<Stemmekrets> = mutableListOf()
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
        val kommune = Kommune(id = this.id, lokalid = lokalId)
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
        kommune.auditInfo.oppdateringsdato = oppdateringsdato
        kommune.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        kommune.flate = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))

        grunnkretser.forEach { kommune.addGrunnkrets(it) }
        stemmekretser.forEach { kommune.addStemmekrets(it) }

        return kommune
    }

    fun withLokalId(lokalId: String): KommuneBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withGrunnkrets(grunnkrets: Grunnkrets): KommuneBuilder {
        this.grunnkretser.add(grunnkrets)
        return this
    }

    fun withStemmekrets(stemmekrets: Stemmekrets): KommuneBuilder {
        this.stemmekretser.add(stemmekrets)
        return this
    }

}
