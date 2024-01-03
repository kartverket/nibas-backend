package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.common.findAndRemove
import no.kartverket.nibas.common.pop
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.KommuneService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DeltGeometriKretsService(
    val kommuneService: KommuneService,
    val kodelisteService: KodelisteService,
) {
    fun getGrunnkretserSomGrenserTilKommunegrense(kommunenr: String, gyldighetsDato: LocalDate): List<Grunnkrets> {
        val grunnkretserForKommune = kommuneService.hentAlleGrunnkretserForKommune(kommunenr, gyldighetsDato)

        return grunnkretserForKommune
            .flatMap { it.getGrenser(gyldighetsDato) }
            .filter { it.grense.erAdministrativGrense() }
            .flatMap { it.grense.grunnkretser(gyldighetsDato) }
            .distinctBy { it.lokalid }
    }

    fun getStemmekretserSomGrenserTilKommunegrense(kommunenr: String, gyldighetsDato: LocalDate): List<Stemmekrets> {
        val stemmekretserForKommune = kommuneService.hentAlleStemmekretserForKommune(kommunenr, gyldighetsDato)
        return stemmekretserForKommune
            .flatMap { it.getGrenser(gyldighetsDato) }
            .filter { it.grense.erAdministrativGrense() }
            .flatMap { it.grense.stemmekretser(gyldighetsDato) }
            .distinctBy { it.lokalid }
    }

    fun getDeltGeometriGrensekoblingerForGrunnkrets(grunnkrets: Grunnkrets, kommunenr: String, gyldighetsDato: LocalDate, nyeAdministrativeGrenser: List<Grense>): List<GrunnkretsGrenseKobling> {
        return lagNyeGrenserForKrets(kommunenr = kommunenr, gyldighetsDato = gyldighetsDato, gamleGrenser = grunnkrets.grenser, nyeAdministrativeGrenser = nyeAdministrativeGrenser)
            .map { it.toGrunnkretsGrenseKobling(grunnkrets) }
    }

    fun getDeltGeometriGrensekoblingerForStemmekrets(stemmekrets: Stemmekrets, kommunenr: String, gyldighetsDato: LocalDate, nyeAdministrativeGrenser: List<Grense>): List<StemmekretsGrenseKobling> {
        return lagNyeGrenserForKrets(kommunenr = kommunenr, gyldighetsDato = gyldighetsDato, gamleGrenser = stemmekrets.grenser, nyeAdministrativeGrenser = nyeAdministrativeGrenser)
            .map { it.toStemmekretsGrenseKobling(stemmekrets) }
    }

    private fun lagNyeGrenserForKrets(kommunenr: String, gyldighetsDato: LocalDate, gamleGrenser: Collection<GrenseKobling>, nyeAdministrativeGrenser: List<Grense>): Collection<DeltGeometriGrense> {
        val grenserPerFlate = gamleGrenser.groupBy { it.grenseKoblingAttributter.flateIndeks }
        return grenserPerFlate.entries.flatMap { (flateIndex, gamleGrenserForFlate) ->
            gamleGrenserForFlate.groupBy { it.grenseKoblingAttributter.hullIndeks }
                .flatMap { (hullIndex, gamleGrenserForHull) ->
                    lagKomplettPolygonAvGrenserForHullOgFlate(flateIndex, hullIndex, kommunenr, gyldighetsDato, gamleGrenserForHull, nyeAdministrativeGrenser)
                }
        }
    }

    /**
     * Denne funksjonen lager et nytt polygon for en gitt krets. Den vil sørge for å bevare samme geometri og kun bytte ut de administrative grensene
     * med nye grenser som er dannet for delt geometri.
     */
    private fun lagKomplettPolygonAvGrenserForHullOgFlate(flateIndex: Int, hullIndex: Int?, kommuneNr: String, gyldighetsDato: LocalDate, gamleGrenser: Collection<GrenseKobling>, nyeAdministrativeGrenser: List<Grense>): List<DeltGeometriGrense> {
        // Vi starter med å først bytte ut relevante administrative grenser med grenser som gjelder for delt geometri
        val nyeGrenser = gamleGrenser.flatMap { gammelGrense ->
            if (erAdministrativGrenseForKommune(gammelGrense.grense, kommuneNr, gyldighetsDato)) {
                finnStiMellomPunkterMedBreddeFoerstSoek(gammelGrense.grense.grense.startPoint, gammelGrense.grense.grense.endPoint, nyeAdministrativeGrenser)
            } else {
                listOf(gammelGrense.grense)
            }
        }

        // Siden vi nå har byttet ut noen grenser må grensekoblingsattributtene også oppdateres, spesielt siden antallet grense
        // kan ha blitt endret. Grensebyttet er ikke alltid 1 til 1.
        var rekkefoelge = 0
        val grenserSomGjenstaar = nyeGrenser.toMutableList()
        val foresteGrense = DeltGeometriGrense(
            grense = grenserSomGjenstaar.pop(),
            grenseKoblingAttributter = GrenseKoblingAttributter(
                rekkefoelge = rekkefoelge++,
                flateIndeks = flateIndex,
                hullIndeks = hullIndex,
                retningMedKlokken = true
            )
        )

        val grenserMedGrenseKobling = mutableListOf(foresteGrense)
        while(grenserSomGjenstaar.isNotEmpty()) {
            val forrigeGrense = grenserMedGrenseKobling.last()
            val forrigeEndepunkt = if(forrigeGrense.grenseKoblingAttributter.retningMedKlokken) forrigeGrense.grense.grense.endPoint else forrigeGrense.grense.grense.startPoint
            val nesteGrense = grenserSomGjenstaar.findAndRemove { it.grense.startPoint == forrigeEndepunkt || it.grense.endPoint == forrigeEndepunkt }
                ?: throw RuntimeException("Fant ikke neste grense når man forsøkte å oppdatere grensekoblingsatributter. Forrige grense sitt endepunkt: $forrigeEndepunkt")

            val retningMedKlokken = nesteGrense.grense.startPoint == forrigeEndepunkt
            val startIndex = if(retningMedKlokken) 0 else nesteGrense.grense.coordinates.lastIndex

            nesteGrense.grense. coordinates[startIndex] = forrigeEndepunkt.coordinate

            val grenseKoblingAttributter = GrenseKoblingAttributter(
                rekkefoelge = rekkefoelge++,
                flateIndeks = flateIndex,
                hullIndeks = hullIndex,
                retningMedKlokken = retningMedKlokken
            )

            grenserMedGrenseKobling += DeltGeometriGrense(nesteGrense, grenseKoblingAttributter)
        }
        return grenserMedGrenseKobling.toList()
    }

    private fun erAdministrativGrenseForKommune(grense: Grense, kommunenr: String, gyldighetsDato: LocalDate): Boolean =
        grense.erAdministrativGrense() && grense.stemmekretser.any {
            kommunenr == it.stemmekrets.getKommune(gyldighetsDato)?.kommunenummer
        }
}

data class DeltGeometriGrense(
    val grense: Grense,
    val grenseKoblingAttributter: GrenseKoblingAttributter
) {
    fun toStemmekretsGrenseKobling(stemmekrets: Stemmekrets): StemmekretsGrenseKobling =
        StemmekretsGrenseKobling(stemmekrets, grense, grenseKoblingAttributter)

    fun toGrunnkretsGrenseKobling(grunnkrets: Grunnkrets): GrunnkretsGrenseKobling =
        GrunnkretsGrenseKobling(grunnkrets, grense, grenseKoblingAttributter)
}
