package no.kartverket.nibas.builders

import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate
import java.time.ZonedDateTime

class FylkeBuilder(
    private val id: String = "1",
    private var objtype: String = "Fylke",
    private var fylkesnummer: String = "UUID-1",
    private var lokalId: Lokalid = Lokalid("123"),
    private var navn: String = "Vestland",
    private var spraak: String = "NOR",
    private var samiskforvaltningsomraade: Boolean = false,
    private var endretdato: ZonedDateTime = ZonedDateTime.now(),
    private var kommuner: List<Kommune> = emptyList(),
    private var version: Int = 1,
    private var gyldigFra: LocalDate = LocalDate.now(),
    private var gyldigTil: LocalDate? = null,
) {

    companion object {
        val DEFAULT_FYLKE = FylkeBuilder().build()
    }

    fun withNavn(navn: String, spraak: String = "NOR"): FylkeBuilder {
        this.navn = navn
        this.spraak = spraak
        return this
    }

    fun build(): Fylke {
        val fylke = Fylke(id = this.id, lokalid = lokalId.value)
        val spraakkode = Spraakkode()
        spraakkode.id = spraak
        spraakkode.description = spraak

        val administrativenhetnavn = Administrativenhetnavn()
        administrativenhetnavn.navn = navn
        administrativenhetnavn.spraak = spraakkode

        fylke.administrativenhetnavn = mutableSetOf(administrativenhetnavn)
        administrativenhetnavn.fylkeFk = fylke

        fylke.objtype = objtype
        fylke.fylkesnummer = fylkesnummer
        fylke.samiskforvaltningsomraade = samiskforvaltningsomraade
        fylke.auditInfo.endretdato = endretdato
        fylke.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        fylke.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))
        fylke.version = version
        fylke.gyldighet = Gyldighet(gyldigFra, gyldigTil)

        kommuner.forEach {
            fylke.addKommune(it)
        }
        return fylke
    }

    fun buildWithoutAdministrativenhetnavn(): Fylke {
        return build().apply { this.administrativenhetnavn.clear() }
    }

    fun withLokalId(lokalId: String): FylkeBuilder {
        this.lokalId = Lokalid(lokalId)
        return this
    }

    fun withFylkesnummer(fylkesnummer: String): FylkeBuilder {
        this.fylkesnummer = fylkesnummer
        return this
    }

    fun withKommune(kommune: Kommune): FylkeBuilder {
        this.kommuner = this.kommuner.plus(kommune)
        return this
    }

    fun withVersion(version: Int): FylkeBuilder {
        this.version = version
        return this
    }

}
