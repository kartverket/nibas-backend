package no.kartverket.nibas.service.bopliktgrensetransformasjon

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.geometry.avrundLineString
import no.kartverket.nibas.common.geometry.createGeometryFactory
import no.kartverket.nibas.domain.BopliktomraadeGrenseKobling
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.repository.BopliktomraadeGrenseKoblingRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.service.BopliktomraadeService
import no.kartverket.nibas.service.FlateService
import no.kartverket.nibas.sporing.AuditInfo
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.springframework.stereotype.Service
import java.time.LocalDate
import jakarta.transaction.Transactional

@Service
class BopliktgrenserTransformasjonService(
    private val grenseRepository: GrenseRepository,
    private val bopliktomraadeService: BopliktomraadeService,
    private val bopliktomraadeGrenseKoblingRepository: BopliktomraadeGrenseKoblingRepository,
    private val flateService: FlateService
) {
    // Henter alle bopliktområder og produserer én grense for hvert polygon ved å bruke polygonet sitt skall.
    // Oppretter også BopliktomraadeGrenseKobling for alle grensene

    //TODO: Legg til håndtering av hull i bopliktområdet sine polygoner.
    @Transactional
    fun transform() {
        resetTables()
        val gf = createGeometryFactory()
        val allBopliktomraader = bopliktomraadeService.hentAlleBopliktomraader(LocalDate.now())
        val allGrensekoblinger = mutableListOf<BopliktomraadeGrenseKobling>()
        val allBopliktgrenser = allBopliktomraader.flatMap { bopliktomraade ->
            val omraade = bopliktomraade.omraade
            val grenserForMultiPolygon = mutableListOf<Grense>()
            if (omraade != null) {
                grenserForMultiPolygon.addAll(shellsOf(omraade).map { shell ->
                    val bopliktomraadegrense = Grense(
                        grensetype = Grensetype.Bopliktgrense,
                    ).apply {
                        setGyldigFra(bopliktomraade.gyldighet.gyldigFra)
                        datafangstdato = bopliktomraade.datafangstdato
                        grense = gf.createLineString(avrundLineString(shell.coordinates))
                        auditInfo = AuditInfo(endretAv = "bopliktgrenserapplication")
                        kvalitet = PosisjonskvalitetEmbeddable("637d1a12-cdeb-4798-8833-1ccfbb23dd5a") //99 Ukjent målemetode
                    }
                    val bopliktomraadeGrenseKobling = BopliktomraadeGrenseKobling(
                        bopliktomraade = bopliktomraade, grense = bopliktomraadegrense
                    )
                    allGrensekoblinger.add(bopliktomraadeGrenseKobling)

                    bopliktomraadegrense
                })
            }
            grenserForMultiPolygon
        }
        grenseRepository.saveAllAndFlush(allBopliktgrenser)
        bopliktomraadeGrenseKoblingRepository.saveAll(allGrensekoblinger)

        allBopliktomraader.forEach { bopliktomraade ->
            allGrensekoblinger
                .filter { it.bopliktomraade.lokalid == bopliktomraade.lokalid }
                .forEach { bkg -> bopliktomraade.addGrensekobling(bkg.grense)
            }
        }

        flateService.oppdaterFlaterFor(allBopliktomraader.toSet())
    }

    private fun resetTables(){
        grenseRepository.deleteAllByGrensetype(Grensetype.Bopliktgrense)
        bopliktomraadeGrenseKoblingRepository.deleteAll()
    }

    private fun shellsOf(multiPolygon: MultiPolygon): List<LineString> {
        return (0 until multiPolygon.numGeometries)
            .map { n -> multiPolygon.getGeometryN(n) }
            .filterIsInstance<Polygon>()
            .map { polygon -> polygon.exteriorRing }
    }
}
