package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.ekstern.GrenserApi
import no.kartverket.nibas.api.v1.ekstern.PaginertRespons
import no.kartverket.nibas.api.v1.ekstern.response.GrenseResponse
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.service.GrenseService
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("EksternGrenserController")
class GrenserController(
    private val grenseService: GrenseService
) : GrenserApi {

    /**
     * Henter alle Kommunegrense, Fylkesgrense, Riksgrense og Territorialgrense
     *
     * @param gyldighetsdato Dato for gyldighet av grensene (standard er dagens dato)
     * @param side Sidenummer for paginering (starter på 0)
     * @param antall Antall resultater per side (standard er 10)
     * @return Paginert liste med Kommunegrense, Fylkesgrense, Riksgrense og Territorialgrense
     */
    override fun hentGrenser(
        gyldighetsdato: LocalDate,
        side: Int,
        antall: Int
    ): PaginertRespons<GrenseResponse> {
        val pageable = PageRequest.of(side, antall)
        val grensePage = grenseService.hentAlleGrenser(gyldighetsdato, pageable)
        val grenseResponses = grensePage.map { GrenseResponse.fraGrense(it) }
        
        return PaginertRespons(
            innhold = grenseResponses.content,
            side = grensePage.number,
            antallPerSide = grensePage.size,
            totaltAntall = grensePage.totalElements,
            totaltAntallSider = grensePage.totalPages
        )
    }

    /**
     * Henter en spesifikk grense basert på lokalid
     *
     * @param lokalid Lokal ID for grensen som skal hentes
     * @return Grense med angitt lokalid
     * @throws NotFoundException hvis grensen ikke finnes
     */
    override fun hentGrense(lokalid: String): GrenseResponse {
        val identifikator = createIdentifikator(lokalid, LocalDate.now())
        val grense = grenseService.hentGrense(identifikator)
            ?: throw NotFoundException("Grense med lokalid $lokalid ble ikke funnet")

        return GrenseResponse.fraGrense(grense)
    }
}
