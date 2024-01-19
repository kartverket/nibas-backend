package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.common.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.pop
import no.kartverket.nibas.common.removeLast
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DeltGeometriAdministrativeGrenserService(
    val kommuneService: KommuneService,
    val kodelisteService: KodelisteService
) {
    private val geometryFactory = SosiGeometryFunctions.getGeometryFactory()

    /**
     * Funksjonen tar inn en kommune (og gyldighetsdato) og returnerer ett sett med nye (administrative) grenser for kommunen
     * som kan brukes av både stemmekretser og grunnkretser. Disse grensene baserer ser i hovedsak på grunnkretsgrensene
     * men noen grenser vil bli splittet for å få på plass nødvendige knutepunkt fra stemmekretsgrensene. Geometrien på de nye grensene
     * vil være lik som før.
     */
    fun getAdministrativeGrenserMedDeltGeometriForKommune(kommune: Kommune, gyldighetsDato: LocalDate): List<Grense> {
        val stemmekretserForKommune = kommune.getStemmekretser(gyldighetsDato)

        val administrativeGrenserForKommune = kommune.getGrensekoblinger(gyldighetsDato).map {
            if (it.grenseKoblingAttributter.retningMedKlokken) it.grense.grense else it.grense.grense.reverse()
        }

        val grensepunkterForGrunnkretser = getGrensepunkterIRiktigRekkefoelge(administrativeGrenserForKommune)
        val administrativeGrenserForStemmekretserIKommunen = stemmekretserForKommune
            .flatMap { it.getGrensekoblinger(gyldighetsDato) }
            .filter { it.grense.erAdministrativGrense() }

        val knutepunktStemmekrets = administrativeGrenserForStemmekretserIKommunen
            .map { if (it.grenseKoblingAttributter.retningMedKlokken) it.grense.grense.startPoint else it.grense.grense.endPoint }
            .map { it.coordinate }

        val knutepunktGrunnkrets = administrativeGrenserForKommune.map { it.startPoint.coordinate }

        val grensepunkterPerPolygon = leggTilManglendeKnutepunktFraStemmekrets(grensepunkterForGrunnkretser, knutepunktStemmekrets)
        return grensepunkterPerPolygon
            .flatMap { grensepunkterTilLineStrings(it, knutepunktGrunnkrets + knutepunktStemmekrets) }
            .map { lineStringTilGrense(it, kommune.getGrensekoblinger(gyldighetsDato) + administrativeGrenserForStemmekretserIKommunen)}
    }

    private fun leggTilManglendeKnutepunktFraStemmekrets(grensepunkterForGrunnkrets: List<List<Coordinate>>, knutepunktStemmekrets: List<Coordinate>): List<List<Coordinate>> {
        val knutepunktSomBareErIStemmekrets = knutepunktStemmekrets.filter { knutepunkt ->
            grensepunkterForGrunnkrets.flatten().none { erSammePunkt(it, knutepunkt, 0.001) }
        }

        val nyeGrensepunkter = grensepunkterForGrunnkrets.map { it.toMutableList() }

        knutepunktSomBareErIStemmekrets.forEach {
            nyeGrensepunkter.finnSirkelMedPunkt(it).insertPunktPaaRiktigPlass(it)
        }

        return nyeGrensepunkter.map { it.toList() }
    }

    private fun MutableList<Coordinate>.insertPunktPaaRiktigPlass(punkt: Coordinate) {
        val indexToInsertAfter = this.indices.find { index ->
            val lineSegment = LineSegment(this[index], this[(index+1) % this.size])
            erPunktPaaLinje(punkt, lineSegment)
        }
        if (indexToInsertAfter != null) {
            this.add(indexToInsertAfter + 1, punkt)
        } else {
            throw RuntimeException("Klarte ikke å finne riktig plass å legge grensepunktet i listen. Gjelder punkt $punkt")
        }
    }

    /**
     * Denne funksjonen tar inn en liste med LineStrings som representerer alle grensene for en krets, og returnerer
     * en liste med lister av koordinater for hvert polygon
     *
     * Den ytterse listen har ett element for hver flate i denne kretsen, og den indre listen er liste med alle koordinater/grensepunkter som danner flaten.
     * Den indre listen har grensepunktene i sortert rekkefølge - slik at om man hadde tegnet en strek fra punkt til punkt vil man ende opp med å tegne flaten.
     */
    private fun getGrensepunkterIRiktigRekkefoelge(grenser: List<LineString>): List<List<Coordinate>> {
        return getGrenserIRiktigRekkefoelge(grenser)
            .map { komplettGrenseSirkel ->
                komplettGrenseSirkel.flatMap {
                    it.coordinateSequence.toCoordinateArray().toList().removeLast()
                }
            }
    }


    private fun List<MutableList<Coordinate>>.finnSirkelMedPunkt(punkt: Coordinate): MutableList<Coordinate> {
        return this.find { grensesirkel ->
            grensesirkel.indices.any { index ->
                val lineSegment = LineSegment(grensesirkel[index], grensesirkel[(index+1) % grensesirkel.size])
                erPunktPaaLinje(punkt, lineSegment)
            }
        } ?: throw RuntimeException("Fant ingen sirkel dette punktet er en del av: $punkt")
    }

    private fun grensepunkterTilLineStrings(grensepunkter: List<Coordinate>, knutepunkter: List<Coordinate>): List<LineString> {
        val lineStrings = mutableListOf<LineString>()
        val resterendeGrensepunkter = grensepunkter.toMutableList()
        var koordinaterForNesteLinestring = mutableListOf(resterendeGrensepunkter.pop())

        while (resterendeGrensepunkter.size > 0) {
            val punkt = resterendeGrensepunkter.pop()
            koordinaterForNesteLinestring.add(punkt)

            if (knutepunkter.any { erSammePunkt(it, punkt, 0.001) }) {
                lineStrings.add(geometryFactory.createLineString(koordinaterForNesteLinestring.toTypedArray()))
                koordinaterForNesteLinestring = mutableListOf(punkt)
            }
        }

        koordinaterForNesteLinestring.add(lineStrings[0].startPoint.coordinate)
        lineStrings.add(geometryFactory.createLineString(koordinaterForNesteLinestring.toTypedArray()))

        return lineStrings.toList()
    }

    private fun lineStringTilGrense(lineString: LineString, gamleGrenser: Collection<GrenseKobling>): Grense =
        gamleGrenser.finnGrensekoblingForGrense(lineString)
            ?.grense?.copyWithNewUUID(lineString)
            ?: throw RuntimeException("Kunne ikke finne en eksisterende grense som matcher linestringen $lineString")
}
