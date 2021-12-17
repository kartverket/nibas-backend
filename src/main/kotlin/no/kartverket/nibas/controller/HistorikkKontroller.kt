package no.kartverket.nibas.controller

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.domain.historikk.GeometriHistorikkObjekt
import no.kartverket.nibas.domain.historikk.HistorikkObjekt
import no.kartverket.nibas.service.historikk.HistorikkService
import org.springframework.beans.factory.annotation.Autowired
import no.kartverket.nibas.api.v1.HistorikkApi as HistorikkApiV1
import org.springframework.web.bind.annotation.RestController

@RestController
class HistorikkKontroller : HistorikkApiV1 {

    @Autowired
    lateinit var historikkService: HistorikkService

    override fun hentHistorikkForAlle(type: AdministrativEnhetType): List<HistorikkObjekt> {
        return historikkService.hentAllHistorikkForType(type)
    }

    override fun hentHistorikkForEn(id: Int, type: AdministrativEnhetType): List<HistorikkObjekt> {
        return historikkService.hentHistorikkForType(id, type)
    }

    override fun hentHistorikkForEnGrense(id: Int, type: AdministrativEnhetType): List<GeometriHistorikkObjekt> {
        return historikkService.hentGrenseHistorikkForType(id, type)
    }
}