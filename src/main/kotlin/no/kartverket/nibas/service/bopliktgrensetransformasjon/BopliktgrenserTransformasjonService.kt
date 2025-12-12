package no.kartverket.nibas.service.bopliktgrensetransformasjon

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.geometry.avrundLineString
import no.kartverket.nibas.common.geometry.createGeometryFactory
import no.kartverket.nibas.domain.BopliktomraadeGrenseKobling
import no.kartverket.nibas.domain.Grense
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
                grenserForMultiPolygon.addAll(ringsOf(omraade).map { ring ->
                    val bopliktomraadegrense = Grense(
                        grensetype = Grensetype.Bopliktgrense,
                    ).apply {
                        setGyldigFra(bopliktomraade.gyldighet.gyldigFra)
                        datafangstdato = bopliktomraade.datafangstdato
                        grense = gf.createLineString(avrundLineString(ring.coordinates))
                        auditInfo = AuditInfo(endretAv = "bopliktgrenserapplication")
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

    private fun ringsOf(multiPolygon: MultiPolygon): List<LineString> {
        return (0 until multiPolygon.numGeometries)
            .asSequence()
            .map { multiPolygon.getGeometryN(it) }
            .filterIsInstance<Polygon>()
            .flatMap { polygon ->
                sequence {
                    yield(polygon.exteriorRing)
                    for (i in 0 until polygon.numInteriorRing) {
                        yield(polygon.getInteriorRingN(i))
                    }
                }
            }
            .toList()
    }

}
