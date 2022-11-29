package no.kartverket.nibas.kodelister

import no.kartverket.nibas.api.v1.KodelisteApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class KodelisteController @Autowired constructor(private val kodelisteService: KodelisteService) : KodelisteApi {

    override fun fetchKommunenumre(): KodelisteRespons {
        return kodelisteService.getKommunenumre()
    }

    override fun fetchFylkesnumre(): KodelisteRespons {
        return kodelisteService.getFylkesnumre()
    }

    override fun fetchMaalemetodeKoder(): KodelisteRespons {
        return kodelisteService.getMaalemetodeKoder()
    }

    override fun fetchLandkoder(): KodelisteRespons {
        return kodelisteService.getLandkoder()
    }

    override fun fetchGrensetyper(): KodelisteRespons {
        return kodelisteService.getGrensetyper()
    }
}

