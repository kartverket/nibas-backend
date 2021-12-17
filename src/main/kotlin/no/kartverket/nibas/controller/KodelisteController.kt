package no.kartverket.nibas.controller

import no.kartverket.nibas.api.v1.KodelisteApi
import no.kartverket.nibas.kodelister.KodelisteItem
import no.kartverket.nibas.kodelister.KodelisteService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class KodelisteController @Autowired constructor(private val kodelisteService: KodelisteService) : KodelisteApi {

    override fun fetchKommunenumre(): List<KodelisteItem> {
        return kodelisteService.getKommunenumre()
    }

    override fun fetchFylkesnumre(): List<KodelisteItem> {
        return kodelisteService.getFylkesnumre()
    }

    override fun fetchMaalemetodeKoder(): List<KodelisteItem> {
        return kodelisteService.getMaalemetodeKoder()
    }

}
