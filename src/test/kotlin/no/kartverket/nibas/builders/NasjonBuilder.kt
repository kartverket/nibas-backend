package no.kartverket.nibas.builders

import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate

class NasjonBuilder(
    private val id: String = "1",
    private var navn: String = "Norge",
    private var spraak: String = "nor",
    private var lokalId: Lokalid = Lokalid("123"),
    private var fylker: List<Fylke> = emptyList(),
    private var gyldigFra: LocalDate = LocalDate.now(),
    private var gyldigTil: LocalDate? = null,
    private var opphav: String = "Eidsvoll"
) {

    companion object {
        val DEFAULT_NASJON = NasjonBuilder().build()
    }

    fun withNavn(navn: String): NasjonBuilder {
        this.navn = navn
        return this
    }

    fun build(): Nasjon {
        val nasjon = Nasjon(id = this.id, lokalid = lokalId.value)
        val spraakkode = Spraakkode()
        spraakkode.id = spraak
        spraakkode.description = spraak

        val administrativenhetnavn = Administrativenhetnavn()
        administrativenhetnavn.navn = navn
        administrativenhetnavn.spraak = spraakkode

        nasjon.administrativenhetnavn = mutableSetOf(administrativenhetnavn)
        nasjon.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        nasjon.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))
        nasjon.gyldighet = Gyldighet(gyldigFra, gyldigTil)
        nasjon.opphav = opphav

        fylker.forEach {
            nasjon.addFylke(it)
        }

        return nasjon
    }

    fun buildWithoutAdministrativenhetnavn(): Nasjon {
        return build().apply { this.administrativenhetnavn.clear() }
    }

    fun withFylke(fylke: Fylke): NasjonBuilder {
        this.fylker = this.fylker.plus(fylke)
        return this
    }

}
