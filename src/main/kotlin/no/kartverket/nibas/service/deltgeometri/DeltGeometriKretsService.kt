package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Stemmekrets
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
            .flatMap { it.getGrensekoblinger(gyldighetsDato) }
            .filter { it.grense.erAdministrativGrense() }
            .flatMap { it.grense.grunnkretser(gyldighetsDato) }
            .distinctBy { it.lokalid }
    }

    fun getStemmekretserSomGrenserTilKommunegrense(kommunenr: String, gyldighetsDato: LocalDate): List<Stemmekrets> {
        val stemmekretserForKommune = kommuneService.hentAlleStemmekretserForKommune(kommunenr, gyldighetsDato)
        return stemmekretserForKommune
            .flatMap { it.getGrensekoblinger(gyldighetsDato) }
            .filter { it.grense.erAdministrativGrense() }
            .flatMap { it.grense.stemmekretser(gyldighetsDato) }
            .distinctBy { it.lokalid }
    }

    private fun erAdministrativGrenseForKommune(grense: Grense, kommunenr: String, gyldighetsDato: LocalDate): Boolean =
        grense.erAdministrativGrense() && grense.stemmekretser(gyldighetsDato).any {
            kommunenr == it.getKommune(gyldighetsDato)?.kommunenummer
        }
}
