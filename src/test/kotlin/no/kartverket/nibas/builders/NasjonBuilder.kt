package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate

class NasjonBuilder(
    private val id: String = "1",
    private var navn: String? = null,
    private var spraak: String = "nor",
    private var unikLokalid: UnikLokalid = UnikLokalid("nasjon123", Flatetype.NASJON),
    private var fylker: List<Fylke> = emptyList(),
    private var gyldigFra: LocalDate = LocalDate.now(),
    private var gyldigTil: LocalDate? = null,
    private var opphav: String = "Eidsvoll",
    private var administrativenhetnavn: List<Administrativenhetnavn> = emptyList()
) {

    companion object {
        val DEFAULT_NASJON = NasjonBuilder().withNavn("Norge").build()
    }

    fun withNavn(navn: String): NasjonBuilder {
        this.navn = navn
        return this
    }

    fun build(): Nasjon {
        val nasjon = buildUtenUnikLokalidOgTilknytninger()
        nasjon.addNasjonTilUnikLokalid(unikLokalid)

        fylker.forEach { nasjon.addFylke(it) }
        administrativenhetnavn.forEach { nasjon.addAdministrativenhetNavn(it) }

        return nasjon
    }

    fun buildUtenUnikLokalidOgTilknytninger(): Nasjon {
        val nasjon = Nasjon(id = this.id, lokalid = unikLokalid.lokalid)

        if (navn != null) {
            val spraakkode = Spraakkode()
            spraakkode.id = spraak
            spraakkode.description = spraak

            val admNavn = Administrativenhetnavn()
            admNavn.navn = navn
            admNavn.spraak = spraakkode

            administrativenhetnavn = listOf(admNavn)
        }

        nasjon.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        nasjon.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))
        nasjon.gyldighet = Gyldighet(gyldigFra, gyldigTil)
        nasjon.opphav = opphav

        return nasjon
    }

    fun withFylke(fylke: Fylke): NasjonBuilder {
        this.fylker = this.fylker.plus(fylke)
        return this
    }

}
