package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate
import java.time.ZonedDateTime

class FylkeBuilder(
    private val id: String = "1",
    private var fylkesnummerUuid: String = "UUID-2",
    private var fylkesnummer: String = "89",
    private var unikLokalid: UnikLokalid = UnikLokalid("fylke123", Flatetype.FYLKE),
    private var navn: String? = null,
    private var spraak: String = "NOR",
    private var samiskforvaltningsomraade: Boolean = false,
    private var endretdato: ZonedDateTime = ZonedDateTime.now(),
    private var kommuner: List<Kommune> = emptyList(),
    private var version: Int = 1,
    private var gyldigFra: LocalDate = LocalDate.now(),
    private var gyldigTil: LocalDate? = null,
    private var administrativenhetnavn: List<Administrativenhetnavn> = emptyList(),
) {

    companion object {
        val DEFAULT_FYLKE = FylkeBuilder().withNavn("Vestland").build()
    }

    fun withNavn(navn: String, spraak: String = "NOR"): FylkeBuilder {
        this.navn = navn
        this.spraak = spraak
        return this
    }

    fun withFylkesnummer(fylkesnummer: String): FylkeBuilder {
        this.fylkesnummer = fylkesnummer
        return this
    }

    fun build(): Fylke {
        val fylke = buildUtenUnikLokalidOgTilknytninger()
        fylke.addFylkeTilUnikLokalid(unikLokalid)

        kommuner.forEach { fylke.addKommune(it) }
        administrativenhetnavn.forEach { fylke.addAdministrativenhetNavn(it) }
        return fylke
    }

    fun buildUtenUnikLokalidOgTilknytninger(): Fylke {
        val fylke = Fylke(id = this.id, lokalid = unikLokalid.lokalid)

        if (navn != null) {
            val spraakkode = Spraakkode()
            spraakkode.id = spraak
            spraakkode.description = spraak

            val admNavn = Administrativenhetnavn()
            admNavn.navn = navn
            admNavn.spraak = spraakkode

            administrativenhetnavn = listOf(admNavn)
        }

        fylke.fylkesnummerUuid = fylkesnummerUuid
        fylke.fylkesnummer = fylkesnummer
        fylke.samiskforvaltningsomraade = samiskforvaltningsomraade
        fylke.auditInfo.endretdato = endretdato
        fylke.posisjon = GeometryFactory().createPoint(Coordinate(1.0, 1.0))
        fylke.omraade = GeometryFactory().createMultiPolygon(arrayOf(GeometryFactory().createPolygon()))
        fylke.version = version
        fylke.gyldighet = Gyldighet(gyldigFra, gyldigTil)

        return fylke
    }

    fun withFylkesnummerUuid(fylkesnummerUuid: String): FylkeBuilder {
        this.fylkesnummerUuid = fylkesnummerUuid
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
