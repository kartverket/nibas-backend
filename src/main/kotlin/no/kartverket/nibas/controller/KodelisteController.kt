package no.kartverket.nibas.controller

import no.kartverket.nibas.api.v1.KodelisteApi
import no.kartverket.nibas.kodelister.KodelisteItem
import no.kartverket.nibas.kodelister.KodelisteService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class KodelisteController @Autowired constructor(private val kodelisteService: KodelisteService) : KodelisteApi {

    override fun fetchGyldigeKommunenumre(): List<KodelisteItem> {
        return kodelisteService.getGyldigeKommunenumre()
    }

    override fun fetchAlleKommunenumre(): List<KodelisteItem> {
        return kodelisteService.getAlleKommunenumre()
    }

    override fun fetchGyldigeFylkesnumre(): List<KodelisteItem> {
        return kodelisteService.getGyldigeFylkesnumre()
    }

    override fun fetchAlleFylkesnumre(): List<KodelisteItem> {
        return kodelisteService.getAlleFylkesnumre()
    }

    override fun fetchMalemetodeKoder(): List<KodelisteItem> {
        return kodelisteService.getMalemetodeKode()
    }

    override fun fetchMalemetoder(): List<KodelisteItem> {
        return kodelisteService.getMalemetode()
    }

}
